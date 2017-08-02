package jce;

import static jce.properties.TextProperty.ECORE_PACKAGE;
import static jce.properties.TextProperty.PROJECT_SUFFIX;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;

import eme.EcoreMetamodelExtraction;
import eme.generator.GeneratedEcoreMetamodel;
import eme.generator.saving.SavingInformation;
import eme.properties.BinaryProperty;
import eme.properties.ExtractionProperties;
import eme.properties.TextProperty;
import jce.codemanipulation.ImportOrganizer;
import jce.codemanipulation.ecore.EcoreImportManipulator;
import jce.codemanipulation.origin.FieldEncapsulator;
import jce.codemanipulation.origin.InheritanceManipulator;
import jce.codemanipulation.origin.MemberRemover;
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
    private final InheritanceManipulator inheritanceManipulator;
    private final EcoreMetamodelExtraction metamodelGenerator;
    private final EcorificationProperties properties;
    private final WrapperGenerator wrapperGenerator;
    private final ImportOrganizer importOrganizer;

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        properties = new EcorificationProperties();
        metamodelGenerator = new EcoreMetamodelExtraction();
        genModelGenerator = new GenModelGenerator();
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
        check(originalProject);
        logger.info("Starting Ecorification...");
        // 1. generate metamodel, GenModel, model code and make Project copy:
        GeneratedEcoreMetamodel metamodel = metamodelGenerator.extractAndSaveFrom(originalProject);
        GenModel genModel = genModelGenerator.generate(metamodel);
        IProject project = getProject(metamodel.getSavingInformation()); // Retrieve output project
        ModelCodeGenerator.generate(genModel, properties);
        // 2. generate wrappers:
        XtendLibraryHelper.addXtendLibs(project, properties);
        ResourceRefresher.refresh(project);
        wrapperGenerator.buildWrappers(metamodel, project);
        // 3. adapt Ecore code
        new EcoreImportManipulator(metamodel, properties).manipulate(project);
        importOrganizer.manipulate(project);
        // 4. adapt origin code:
        fieldEncapsulator.manipulate(project);
        new MemberRemover(metamodel, properties).manipulate(project);
        importOrganizer.manipulate(project);
        inheritanceManipulator.manipulate(project);
        // 5. build project and make changes visible in the Eclipse IDE:
        rebuild(project, properties);
        logger.info("Ecorification complete!");
    }

    /**
     * Checks whether a specific {@link IProject} is valid (neither null nor nonexistent)
     * @param project is the specific {@link IProject}.
     */
    private void check(IProject project) {
        if (project == null) {
            throw new IllegalArgumentException("Project can't be null!");
        } else if (!project.exists()) {
            throw new IllegalArgumentException("Project " + project.toString() + "does not exist!");
        }
    }

    /**
     * Configures the extraction properties. JCE Properties are referenced directly, EME properties are referenced witht
     * the class name.
     */
    private void configureExtraction(ExtractionProperties properties) {
        properties.set(TextProperty.SAVING_STRATEGY, "CopyProject");
        properties.set(TextProperty.PROJECT_SUFFIX, this.properties.get(PROJECT_SUFFIX));
        properties.set(TextProperty.DEFAULT_PACKAGE, this.properties.get(ECORE_PACKAGE));
        properties.set(TextProperty.DATATYPE_PACKAGE, "datatypes");
        properties.set(BinaryProperty.DUMMY_CLASS, false);
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
     * Tries to build the project.
     */
    private void rebuild(IProject project, EcorificationProperties properties) {
        ResourceRefresher.refresh(project);
        IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties);
        try {
            project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
    }
}