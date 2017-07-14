package jce;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
import jce.util.ResourceRefresher;

/**
 * Main class for Java code ecorification.
 * @author Timur Saglam
 */
public class JavaCodeEcorification {
    private static final Logger logger = LogManager.getLogger(JavaCodeEcorification.class.getName());
    private final ExtractionProperties extractionProperties;
    private final GenModelGenerator genModelGenerator;
    private final EcoreMetamodelExtraction metamodelGenerator;
    private final InheritanceManipulator inheritanceManipulator;
    private final FieldEncapsulator fieldEncapsulator;
    private static final String ECORE_PACKAGE_NAME = "ecore";
    private static final String WRAPPER_PACKAGE_NAME = "wrappers";

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        metamodelGenerator = new EcoreMetamodelExtraction();
        genModelGenerator = new GenModelGenerator();
        extractionProperties = metamodelGenerator.getProperties();
        extractionProperties.set(TextProperty.SAVING_STRATEGY, "CopyProject");
        extractionProperties.set(TextProperty.PROJECT_SUFFIX, "Ecorified");
        extractionProperties.set(TextProperty.DEFAULT_PACKAGE, ECORE_PACKAGE_NAME);
        extractionProperties.set(TextProperty.DATATYPE_PACKAGE, "datatypes");
        extractionProperties.set(BinaryProperty.DUMMY_CLASS, false);
        inheritanceManipulator = new InheritanceManipulator(ECORE_PACKAGE_NAME, WRAPPER_PACKAGE_NAME);
        fieldEncapsulator = new FieldEncapsulator(ECORE_PACKAGE_NAME, WRAPPER_PACKAGE_NAME);
    }

    /**
     * Starts the ecorification for a specific Java project.
     * @param project is the specific Java project as {@link IProject}.
     */
    public void start(IProject project) {
        // Initialize:
        check(project);
        logger.info("Starting Ecorification...");
        // Generate metamodel, GenModel, model code and make Project copy:
        GeneratedEcoreMetamodel metamodel = metamodelGenerator.extractAndSaveFrom(project);
        GenModel genModel = genModelGenerator.generate(metamodel);
        IProject copy = getProject(metamodel.getSavingInformation()); // Retrieve output project
        ModelCodeGenerator.generate(genModel);
        // Generate wrappers and edit classes:
        XtendLibraryHelper.addXtendLibs(copy);
        ResourceRefresher.refresh(copy);
        WrapperGenerator.buildWrappers(metamodel, copy);
        fieldEncapsulator.manipulate(copy);
        inheritanceManipulator.manipulate(copy);
        rebuild(copy);
        // make changes visible in the Eclipse IDE:
        ResourceRefresher.refresh(copy);
        logger.info("Ecorification complete!");
    }

    private void rebuild(IProject project) {
        try { // TODO (MEDIUM) fix Xtend build.
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
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
}