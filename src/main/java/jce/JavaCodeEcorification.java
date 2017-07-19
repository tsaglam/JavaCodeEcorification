package jce;

import java.io.File;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;

import eme.EcoreMetamodelExtraction;
import eme.generator.GeneratedEcoreMetamodel;
import eme.generator.saving.SavingInformation;
import eme.properties.BinaryProperty;
import eme.properties.ExtractionProperties;
import eme.properties.TextProperty;
import jce.codegen.GenModelGenerator;
import jce.codegen.ModelCodeGenerator;
import jce.codegen.WrapperGenerator;
import jce.codegen.XtendLibraryHelper;
import jce.manipulation.FieldEncapsulator;
import jce.manipulation.InheritanceManipulator;
import jce.manipulation.MemberRemover;
import jce.util.ProgressMonitorAdapter;
import jce.util.ResourceRefresher;

/**
 * Main class for Java code ecorification.
 * @author Timur Saglam
 */
public class JavaCodeEcorification {
    private static final String ECORE_PACKAGE = "ecore";
    private static final Logger logger = LogManager.getLogger(JavaCodeEcorification.class.getName());
    private static final String WRAPPER_PACKAGE = "wrappers";
    private final ExtractionProperties extractionProperties;
    private final FieldEncapsulator fieldEncapsulator;
    private final GenModelGenerator genModelGenerator;
    private final InheritanceManipulator inheritanceManipulator;
    private final MemberRemover memberRemover;
    private final EcoreMetamodelExtraction metamodelGenerator;

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        metamodelGenerator = new EcoreMetamodelExtraction();
        genModelGenerator = new GenModelGenerator();
        extractionProperties = metamodelGenerator.getProperties();
        extractionProperties.set(TextProperty.SAVING_STRATEGY, "CopyProject");
        extractionProperties.set(TextProperty.PROJECT_SUFFIX, "Ecorified");
        extractionProperties.set(TextProperty.DEFAULT_PACKAGE, ECORE_PACKAGE);
        extractionProperties.set(TextProperty.DATATYPE_PACKAGE, "datatypes");
        extractionProperties.set(BinaryProperty.DUMMY_CLASS, false);
        inheritanceManipulator = new InheritanceManipulator(ECORE_PACKAGE, WRAPPER_PACKAGE);
        fieldEncapsulator = new FieldEncapsulator(ECORE_PACKAGE, WRAPPER_PACKAGE);
        memberRemover = new MemberRemover(ECORE_PACKAGE, WRAPPER_PACKAGE);
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
        ModelCodeGenerator.generate(genModel);
        // 2. generate wrappers:
        XtendLibraryHelper.addXtendLibs(project);
        ResourceRefresher.refresh(project);
        WrapperGenerator.buildWrappers(metamodel, project);
        // 3. adapt origin code:
        fieldEncapsulator.manipulate(project);
        memberRemover.manipulate(project);
        inheritanceManipulator.manipulate(project);
        // 4. build project and make changes visible in the Eclipse IDE:
        rebuild(project);
        ResourceRefresher.refresh(project);
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
    private void rebuild(IProject project) {
        ResourceRefresher.refresh(project);
        try { // TODO (MEDIUM) fix Xtend build.
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            IFolder xtendFolder = project.getFolder("src" + File.separator + "main" + File.separator + "xtend-gen");
            ResourceRefresher.refresh(project);
            xtendFolder.delete(true, new ProgressMonitorAdapter(logger));
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
    }
}