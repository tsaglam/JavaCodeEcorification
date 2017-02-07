package jce;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;

import eme.EcoreMetamodelExtraction;
import eme.generator.GeneratedEcoreMetamodel;
import eme.properties.TextProperty;
import jce.codegen.GenModelGenerator;
import jce.codegen.ModelCodeGenerator;
import jce.codegen.WrapperManager;

/**
 * Main class for Java code ecorification.
 * @author Timur Saglam
 */
public class JavaCodeEcorification {
    private static final Logger logger = LogManager.getLogger(JavaCodeEcorification.class.getName());
    private final GenModelGenerator genModelGenerator;
    private final EcoreMetamodelExtraction metamodelGenerator;

    /**
     * Basic constructor.
     */
    public JavaCodeEcorification() {
        metamodelGenerator = new EcoreMetamodelExtraction();
        genModelGenerator = new GenModelGenerator();
    }

    public void start(IProject project) {
        check(project);
        logger.info("Starting Ecorification...");
        metamodelGenerator.getProperties().set(TextProperty.SAVING_STRATEGY, "SameProject");
        GeneratedEcoreMetamodel metamodel = metamodelGenerator.extractAndSaveFrom(project);
        GenModel genModel = genModelGenerator.generate(metamodel);
        ModelCodeGenerator.generate(genModel);
        new WrapperManager(metamodel, genModel).buildWrappers();
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
}
