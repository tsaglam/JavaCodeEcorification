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
public final class WrapperManager {
    private static ProjectDirectories directories;
    private static final Logger logger = LogManager.getLogger(WrapperManager.class.getName());
    private static final PathHelper PACKAGE = new PathHelper('.');
    private static final PathHelper PATH = new PathHelper(File.separatorChar);
    private static final WrapperGenerator WRAPPER_GENERATOR = new WrapperGenerator();

    private WrapperManager() {
        // private constructor.
    }

    /**
     * Builds the wrapper classes.
     * @param metamodel is the metamodel that got extracted from the original project.
     * @param directories is the {@link ProjectDirectories} instance for the project.
     */
    public static void buildWrappers(GeneratedEcoreMetamodel metamodel, ProjectDirectories directories) {
        logger.info("Starting the wrapper class generation...");
        WrapperManager.directories = directories;
        buildWrappers(metamodel.getRoot(), "");
        refreshSourceFolder(); // makes wrappers visible in Eclipse
    }

    /**
     * Recursive method for the wrapper creation.
     * @param ePackage is the current {@link EPackage} to create wrappers for.
     * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
     */
    private static void buildWrappers(EPackage ePackage, String path) {
        for (EClassifier eClassifier : ePackage.getEClassifiers()) { // for every classifier
            if (eClassifier instanceof EClass) { // if class
                createXtendWrapper(path, eClassifier.getName()); // create wrapper class
            }
        }
        for (EPackage eSubpackage : ePackage.getESubpackages()) { // for every subpackage
            buildWrappers(eSubpackage, PATH.append(path, eSubpackage.getName())); // do the same
        }
    }

    /**
     * Creates and Xtend wrapper class at a specific location with a specific name.
     * @param packagePath is the path of the specific location.
     * @param name is the name of the wrapper to generate.
     */
    private static void createXtendWrapper(String packagePath, String name) {
        String filePath = PATH.append(directories.getSourceDirectory(), "wrappers", packagePath, name + "Wrapper.xtend");
        String currentPackage = packagePath.replace(File.separatorChar, '.');
        String wrapperPackage = PACKAGE.append("wrappers", currentPackage);
        String ecorePackage = PACKAGE.append("ecore", currentPackage);
        String factoryName = PACKAGE.nameOf(currentPackage) + "Factory";
        factoryName = factoryName.substring(0, 1).toUpperCase() + factoryName.substring(1);
        File file = new File(filePath);
        if (file.exists()) {
            throw new IllegalArgumentException("File already exists: " + filePath);
        }
        file.getParentFile().mkdirs(); // ensure folder tree exists
        write(file, WRAPPER_GENERATOR.generate(name, factoryName, wrapperPackage, ecorePackage));
    }

    /**
     * Refreshes the source folder where the wrappers are generated in.
     */
    private static void refreshSourceFolder() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IContainer folder = root.getContainerForLocation(new Path(directories.getSourceDirectory()));
        try {
            folder.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException exception) {
            logger.warn("Could not refresh source folder. Try that manually.", exception);
        }
    }

    /**
     * Writes a String to a {@link File}.
     * @param file is the {@link File}.
     * @param content is the content String.
     */
    private static void write(File file, String content) {
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}