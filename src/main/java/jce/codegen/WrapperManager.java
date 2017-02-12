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
import jce.util.PathHelper;
import jce.util.ProjectDirectories;

/**
 * Creates and manages wrappers for the classes of the orginal Java project with is ecorified.
 * @author Timur Saglam
 */
public class WrapperManager {
    private static final Logger logger = LogManager.getLogger(WrapperManager.class.getName());
    private static final char SLASH = File.separatorChar;
    private final ProjectDirectories directories;
    private final GeneratedEcoreMetamodel metamodel;
    private final PathHelper packageHelper;
    private final PathHelper pathHelper;
    private final WrapperGenerator wrapperGenerator;

    /**
     * Basic constructor.
     * @param metamodel is the metamodel that got extracted from the original project.
     * @param genModel is the {@link GenModel} that was build for the metamodel.
     */
    public WrapperManager(GeneratedEcoreMetamodel metamodel, ProjectDirectories directories) {
        this.directories = directories;
        this.metamodel = metamodel;
        pathHelper = new PathHelper(SLASH);
        packageHelper = new PathHelper('.');
        wrapperGenerator = new WrapperGenerator();
    }

    /**
     * Builds the wrapper classes.
     */
    public void buildWrappers() {
        logger.info("Starting the wrapper class generation...");
        buildWrappers(metamodel.getRoot(), "");
        refreshSourceFolder(); // makes wrappers visible in Eclipse
    }

    /**
     * Recursive method for the wrapper creation.
     * @param ePackage is the current {@link EPackage} to create wrappers for.
     * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
     */
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

    /**
     * Creates and Xtend wrapper class at a specific location with a specific name.
     * @param packagePath is the path of the specific location.
     * @param name is the name of the wrapper to generate.
     */
    private void createXtendWrapper(String packagePath, String name) {
        String filePath = pathHelper.append(directories.getSourceDirectory(), "wrappers", packagePath, name + "Wrapper.xtend");
        String currentPackage = packagePath.replace(SLASH, '.');
        String wrapperPackage = packageHelper.append("wrappers", currentPackage);
        String ecorePackage = packageHelper.append("ecore", currentPackage);
        String factoryName = packageHelper.nameOf(currentPackage) + "Factory";
        factoryName = factoryName.substring(0, 1).toUpperCase() + factoryName.substring(1);
        File file = new File(filePath);
        if (file.exists()) {
            throw new IllegalArgumentException("File already exists: " + filePath);
        }
        file.getParentFile().mkdirs(); // ensure folder tree exists
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(wrapperGenerator.generate(name, factoryName, wrapperPackage, ecorePackage));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Refreshes the source folder where the wrappers are generated in.
     */
    private void refreshSourceFolder() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IContainer folder = root.getContainerForLocation(new Path(directories.getSourceDirectory()));
        try {
            folder.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException exception) {
            logger.warn("Could not refresh source folder. Try that manually.", exception);
        }
    }
}