package jce.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import eme.generator.GeneratedEcoreMetamodel;

public class WrapperManager {
    private static final Logger logger = LogManager.getLogger(WrapperManager.class.getName());
    private static final char SLASH = File.separatorChar;
    private final GeneratedEcoreMetamodel metamodel;
    private final String workspacePath;
    private final String sourcePath;
    private final WrapperGenerator wrapperGenerator;
    private final PathHelper pathHelper;
    private final PathHelper packageHelper;

    public WrapperManager(GeneratedEcoreMetamodel metamodel, GenModel genModel) {
        this.metamodel = metamodel;
        pathHelper = new PathHelper(SLASH);
        packageHelper = new PathHelper('.');
        workspacePath = pathHelper.nthParentOf(metamodel.getSavingInformation().getFilePath(), 3);
        sourcePath = workspacePath + genModel.getModelDirectory();
        wrapperGenerator = new WrapperGenerator();
    }

    public void buildWrappers() {
        logger.info("Starting the wrapper class generation...");
        buildWrappers(metamodel.getRoot(), "");
        refreshSourceFolder();
    }

    private void buildWrappers(EPackage ePackage, String path) {
        for (EClassifier eClassifier : ePackage.getEClassifiers()) { // for every classifier
            if (eClassifier instanceof EClass) { // if class
                createXtendWrapper(path, eClassifier.getName()); // create wrapper class
            }
        }
        for (EPackage eSubpackage : ePackage.getESubpackages()) { // for every subpackage
            buildWrappers(eSubpackage, pathHelper.append(path, eSubpackage.getName())); // do the same
        }
    }

    private void createXtendWrapper(String packagePath, String name) {
        System.err.println("path = " + packagePath);
        String filePath = '/' + pathHelper.append(sourcePath, "wrappers", packagePath, name + "Wrapper.xtend");
        String codePackage = packagePath.replace(SLASH, '.');
        String wrapperPackage = packageHelper.append("wrappers", codePackage);
        String ecorePackage = packageHelper.append("ecore", codePackage);
        File file = new File(filePath);
        if (file.exists()) {
            throw new IllegalArgumentException("File already exists: " + filePath);
        }
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file); // TODO (HIGH) Factory name
            fileWriter.write(wrapperGenerator.generate(name, "", wrapperPackage, ecorePackage));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void refreshSourceFolder() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IContainer folder = root.getContainerForLocation(new Path(sourcePath));
        try {
            folder.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException exception) {
            logger.warn("Could not refresh source folder. Try that manually.", exception);
        }
    }
}