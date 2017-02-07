package jce.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import eme.generator.GeneratedEcoreMetamodel;

public class WrapperManager {
    private static final Logger logger = LogManager.getLogger(WrapperManager.class.getName());
    private static final String SLASH = File.separator;
    private GenModel genModel;
    private GeneratedEcoreMetamodel metamodel;
    private String workspacePath;
    private WrapperGenerator wrapperGenerator;

    public WrapperManager(GeneratedEcoreMetamodel metamodel, GenModel genModel) {
        this.metamodel = metamodel;
        this.genModel = genModel;
        String modelPath = metamodel.getSavingInformation().getFilePath(); // TODO (MEDIUM) replace / w File.separator
        String projectPath = modelPath.substring(0, modelPath.lastIndexOf(SLASH, modelPath.lastIndexOf(SLASH) - 1));
        workspacePath = projectPath.substring(0, projectPath.lastIndexOf(SLASH));
        wrapperGenerator = new WrapperGenerator();
    }

    public void buildWrappers() {
        logger.info("Starting the wrapper class generation...");
        buildWrappers(metamodel.getRoot(), "wrappers");
    }

    public void createXtendWrapper(String packagePath, String name) {
        String filePath = workspacePath + genModel.getModelDirectory() + SLASH + packagePath + name + "Wrapper.xtend";
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            throw new IllegalArgumentException("File already exists: " + filePath);
        }
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(wrapperGenerator.generate(name, name + "Factory", packagePath));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void buildWrappers(EPackage ePackage, String path) {
        for (EClassifier eClassifier : ePackage.getEClassifiers()) { // for every classifier
            if (eClassifier instanceof EClass) { // if class
                createXtendWrapper(path + SLASH, eClassifier.getName()); // create wrapper class
            }
        }
        for (EPackage eSubpackage : ePackage.getESubpackages()) { // for every subpackage
            buildWrappers(eSubpackage, path + SLASH + eSubpackage.getName()); // do the same
        }
    }
}