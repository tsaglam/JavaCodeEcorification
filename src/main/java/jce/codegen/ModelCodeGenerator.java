package jce.codegen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;

import jce.util.MonitorAdapter;

/**
 * Class for code generation (e.g generating Java code from Ecore GenModels).
 */
public final class ModelCodeGenerator {
    private static final Logger logger = LogManager.getLogger(ModelCodeGenerator.class.getName());

    private ModelCodeGenerator() {
        // private constructor.
    }

    /**
     * Uses a specific GenModel to generate Java Code.
     * @param genModel is the specific GenModel.
     */
    public static void generate(GenModel genModel) {
        if (genModel == null) {
            throw new IllegalArgumentException("GenModel cannot be null to generate code from it");
        }
        genModel.setCanGenerate(true); // allow generation
        Generator generator = new Generator(); // create generator
        generator.setInput(genModel); // set the model-level input object
        generator.generate(genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, new MonitorAdapter(logger));
        logger.info("Generated Java code from GenModel in: " + generator.getGeneratedOutputs().toString());
    }
}