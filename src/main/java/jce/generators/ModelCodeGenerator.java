package jce.generators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;

import jce.properties.EcorificationProperties;
import jce.util.logging.MonitorFactory;

/**
 * Class for code generation (e.g generating Java code from Ecore GenModels).
 */
public final class ModelCodeGenerator {
    private static final Logger logger = LogManager.getLogger(ModelCodeGenerator.class.getName());

    private ModelCodeGenerator() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Uses a specific GenModel to generate Java Code.
     * @param genModel is the specific GenModel.
     * @param properties are the Ecorification properties.
     */
    public static void generate(GenModel genModel, EcorificationProperties properties) {
        if (genModel == null) {
            throw new IllegalArgumentException("GenModel cannot be null to generate code from it");
        }
        genModel.setCanGenerate(true); // allow generation
        Generator generator = new Generator(); // create generator
        generator.setInput(genModel); // set the model-level input object
        generator.generate(genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, MonitorFactory.createMonitor(logger, properties));
        logger.info("Generated Java code from GenModel in: " + generator.getGeneratedOutputs().toString());
    }
}