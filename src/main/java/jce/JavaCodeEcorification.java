package jce;

import static jce.properties.TextProperty.ECORE_PACKAGE;
import static jce.properties.TextProperty.PROJECT_SUFFIX;
import static jce.properties.TextProperty.ROOT_CONTAINER;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import eme.EcoreMetamodelExtraction;
import eme.generator.GeneratedEcoreMetamodel;
import eme.generator.saving.SavingInformation;
import eme.properties.BinaryProperty;
import eme.properties.ExtractionProperties;
import eme.properties.TextProperty;
import jce.codemanipulation.ImportOrganizer;
import jce.codemanipulation.ecore.EcoreImportManipulator;
import jce.codemanipulation.ecore.FactoryRenamer;
import jce.codemanipulation.origin.ClassExposer;
import jce.codemanipulation.origin.DefaultConstructorGenerator;
import jce.codemanipulation.origin.FieldEncapsulator;
import jce.codemanipulation.origin.InheritanceManipulator;
import jce.codemanipulation.origin.MemberRemover;
import jce.generators.EcoreFactoryGenerator;
import jce.generators.GenModelGenerator;
import jce.generators.ModelCodeGenerator;
import jce.generators.WrapperGenerator;
import jce.generators.XtendLibraryHelper;
import jce.properties.EcorificationProperties;
import jce.util.ResourceRefresher;
import jce.util.logging.MonitorFactory;

/**
 * Main class for Java code ecorification.
 * @author Timur Saglam
 */
public class JavaCodeEcorification {
    private static final Logger logger = LogManager.getLogger(JavaCodeEcorification.class.getName());
    private final FieldEncapsulator fieldEncapsulator;
    private final GenModelGenerator genModelGenerator;
    private final ImportOrganizer importOrganizer;
    private final InheritanceManipulator inheritanceManipulator;
    private final EcoreMetamodelExtraction metamodelGenerator;
    private final EcorificationProperties properties;
    private final WrapperGenerator wrapperGenerator;

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        properties = new EcorificationProperties();
        metamodelGenerator = new EcoreMetamodelExtraction();
        genModelGenerator = new GenModelGenerator(properties);
        wrapperGenerator = new WrapperGenerator(properties);
        fieldEncapsulator = new FieldEncapsulator(properties);
        importOrganizer = new ImportOrganizer(properties);
        inheritanceManipulator = new InheritanceManipulator(properties);
        configureExtraction(metamodelGenerator.getProperties());
    }

    /**
     * Starts the ecorification for a specific Java project. Initializes the different steps of the Ecorification
     * pipeline: The extraction of an Ecore metamodel, the Ecore model code generation, the wrapper generation and the
     * origin code adaption.
     * @param originalProject is the specific Java project as {@link IProject}.
     */
    public void start(IProject originalProject) {
        // 0. initialize:
        SourceFolderAnalyzer.verify(originalProject, properties);
        logger.info("Starting Ecorification...");
        GeneratedEcoreMetamodel metamodel = extractMetamodel(originalProject); // 1
        IProject project = getProject(metamodel.getSavingInformation()); // 1,5. Retrieve output project
        buildFactories(metamodel, project); // 2.
        generateWrappers(metamodel, project); // 3.
        new EcoreImportManipulator(metamodel, properties).manipulate(project);  // 4. adapt imports
        adaptOriginCode(metamodel, project); // 5.
        // 6. build project and make changes visible in the Eclipse IDE. Notify the user:
        rebuild(project, properties);
        notifyUser(originalProject);
    }

    /**
     * Encapsulates all fields of the origin code. Removes public, non-static fields and their access methods, organizes
     * all imports, manipulates the inheritance relations to extend the wrappers, creates default constructors where
     * they are missing.
     * @param metamodel is the {@link GeneratedEcoreMetamodel}.
     * @param project is the {@link IProject} where the metamodel is located.
     */
    private void adaptOriginCode(GeneratedEcoreMetamodel metamodel, IProject project) {
        fieldEncapsulator.manipulate(project);
        new MemberRemover(metamodel, properties).manipulate(project);
        importOrganizer.manipulate(project);
        inheritanceManipulator.manipulate(project);
        new DefaultConstructorGenerator(properties).manipulate(project);
    }

    /**
     * Builds the custom Ecore factories, while renaming the old ones.
     * @param metamodel is the {@link GeneratedEcoreMetamodel}.
     * @param project is the {@link IProject} where the metamodel is located.
     */
    private void buildFactories(GeneratedEcoreMetamodel metamodel, IProject project) {
        new FactoryRenamer(metamodel, properties).manipulate(project);
        new EcoreFactoryGenerator(properties).buildFactories(metamodel, project);
        new ClassExposer(properties).manipulate(project);
    }

    /**
     * Configures the extraction properties. JCE Properties are referenced directly, EME properties are referenced witht
     * the class name.
     */
    private void configureExtraction(ExtractionProperties properties) {
        properties.set(TextProperty.PROJECT_SUFFIX, this.properties.get(PROJECT_SUFFIX));
        properties.set(TextProperty.DEFAULT_PACKAGE, this.properties.get(ECORE_PACKAGE));
        properties.set(TextProperty.ROOT_NAME, this.properties.get(ROOT_CONTAINER));
        properties.set(TextProperty.SAVING_STRATEGY, "CopyProject");
        properties.set(TextProperty.DATATYPE_PACKAGE, "datatypes");
        properties.set(BinaryProperty.DUMMY_CLASS, false);
        properties.set(BinaryProperty.ROOT_CONTAINER, true);
        properties.set(BinaryProperty.FINAL_AS_UNCHANGEABLE, false);
        properties.set(BinaryProperty.NESTED_TYPES, false);
        properties.set(BinaryProperty.PARAMETER_MULTIPLICITIES, false);
    }

    /**
     * Extracts a Ecore metamodel in form of an {@link GeneratedEcoreMetamodel} from the original {@link IProject}.
     * Generates a {@link GenModel}.
     * @param originalProject the original {@link IProject}.
     * @return the {@link GeneratedEcoreMetamodel}.
     */
    private GeneratedEcoreMetamodel extractMetamodel(IProject originalProject) {
        GeneratedEcoreMetamodel metamodel = metamodelGenerator.extract(originalProject);
        GenModel genModel = genModelGenerator.generate(metamodel);
        ModelCodeGenerator.generate(genModel, properties);
        return metamodel;
    }

    /**
     * Generates the wrappers, which are the classes that unify the origin code with the Ecore code.
     * @param metamodel is the {@link GeneratedEcoreMetamodel}.
     * @param project is the {@link IProject} where the metamodel is located.
     */
    private void generateWrappers(GeneratedEcoreMetamodel metamodel, IProject project) {
        XtendLibraryHelper.addXtendLibs(project, properties);
        ResourceRefresher.refresh(project);
        wrapperGenerator.buildWrappers(metamodel, project);
    }

    /**
     * Gets {@link IProject} from {@link SavingInformation}.
     */
    private IProject getProject(SavingInformation information) {
        String name = information.getProjectName(); // get project name
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (IProject project : root.getProjects()) { // for every project
            if (project.getName().equals(name)) { // compare with name
                ResourceRefresher.refresh(project);
                return project;
            }
        }
        return null;
    }

    /**
     * Tells the user the ecorification of an {@link IProject} is complete.
     */
    private void notifyUser(IProject project) {
        logger.info("Ecorification complete!");
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        String message = "Ecorification of " + project.getName() + " complete!";
        MessageDialog.openInformation(shell, "Java Code Ecorification", message);
    }

    /**
     * Tries to build the project.
     */
    private void rebuild(IProject project, EcorificationProperties properties) {
        ResourceRefresher.refresh(project);
        IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties);
        try {
            project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }
}