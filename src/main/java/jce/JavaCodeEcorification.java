package jce;

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
import jce.codemanipulation.ImportOrganizer;
import jce.codemanipulation.ecore.EcoreImportManipulator;
import jce.codemanipulation.ecore.FactoryImplementationRenamer;
import jce.codemanipulation.ecore.FactoryRenamer;
import jce.codemanipulation.ecore.PackageImplFactoryCorrector;
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
    private final GenModelGenerator genModelGenerator;
    private final ImportOrganizer importOrganizer;
    private final EcoreMetamodelExtraction metamodelGenerator;
    private final EcorificationProperties properties;
    private final WrapperGenerator wrapperGenerator;

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        properties = new EcorificationProperties();
        metamodelGenerator = new EcorificationExtraction(properties);
        genModelGenerator = new GenModelGenerator(properties);
        wrapperGenerator = new WrapperGenerator(properties);
        importOrganizer = new ImportOrganizer(properties);
    }

    /**
     * Starts the ecorification for a specific Java project. Initializes the
     * different steps of the Ecorification pipeline: The extraction of an Ecore
     * metamodel, the Ecore model code generation, the wrapper generation and the
     * origin code adaption.
     * @param originalProject is the specific Java project as {@link IProject}.
     */
    public void start(IProject originalProject) {
        SourceFolderAnalyzer.verify(originalProject, properties); // 0. initialize:
        logger.info("Starting Ecorification...");
        GeneratedEcoreMetamodel metamodel = extractMetamodel(originalProject); // 1
        IProject project = getProject(metamodel.getSavingInformation()); // 1.5. Retrieve output project
        buildFactories(metamodel, project); // 2.
        generateWrappers(metamodel, project); // 3.
        manipulateEcoreImports(metamodel, project); // 4.
        adaptOriginCode(metamodel, project); // 5.
        finish(project); // 6.
    }

    /**
     * 5. Encapsulates all fields of the origin code. Removes public, non-static
     * fields and their access methods, organizes all imports, manipulates the
     * inheritance relations to extend the wrappers, creates default constructors
     * where they are missing.
     */
    private void adaptOriginCode(GeneratedEcoreMetamodel metamodel, IProject project) {
        new FieldEncapsulator(metamodel.getIntermediateModel(), properties).manipulate(project);
        new MemberRemover(metamodel, properties).manipulate(project);
        importOrganizer.manipulate(project);
        new InheritanceManipulator(metamodel.getIntermediateModel(), properties).manipulate(project);
    }

    /**
     * 2. Builds the custom Ecore factories, while renaming the old ones.
     */
    private void buildFactories(GeneratedEcoreMetamodel metamodel, IProject project) {
        new FactoryRenamer(metamodel, properties).manipulate(project);
        new FactoryImplementationRenamer(metamodel, properties).manipulate(project);
        new DefaultConstructorGenerator(properties, metamodel.getIntermediateModel()).manipulate(project);
        new EcoreFactoryGenerator(properties).buildFactories(metamodel, project);
        new PackageImplFactoryCorrector(metamodel, properties).manipulate(project);
        new ClassExposer(metamodel.getIntermediateModel(), properties).manipulate(project);
    }

    /**
     * 1. Extracts a Ecore metamodel in form of an {@link GeneratedEcoreMetamodel}
     * from the original {@link IProject}. Generates a {@link GenModel}.
     */
    private GeneratedEcoreMetamodel extractMetamodel(IProject originalProject) {
        GeneratedEcoreMetamodel metamodel = metamodelGenerator.extract(originalProject);
        GenModel genModel = genModelGenerator.generate(metamodel);
        ModelCodeGenerator.generate(genModel, properties);
        return metamodel;
    }

    /**
     * 6. Finishes the ecorification: Organizes all imports, rebuilds the project
     * and notifies the user.
     */
    private void finish(IProject project) {
        importOrganizer.manipulate(project);
        rebuild(project, properties);
        notifyUser(project);
    }

    /**
     * 3. Generates the wrappers, which are the classes that unify the origin code
     * with the Ecore code.
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
     * 4. Manipulates the imports of the Ecore code. Every Ecore interface and every
     * correlating implementation class will use the origin code types instead of
     * ecore code types.
     */
    private void manipulateEcoreImports(GeneratedEcoreMetamodel metamodel, IProject project) {
        new EcoreImportManipulator(metamodel, properties).manipulate(project); // 4. adapt imports
    }

    /**
     * Tells the user the ecorification of an {@link IProject} is complete.
     */
    private void notifyUser(IProject project) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        String title = "Ecorification complete!";
        String message = "Ecorification of complete! The ecorified code can be found in the project " + project.getName();
        logger.info(title + " " + message);
        MessageDialog.openInformation(shell, title, message);
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