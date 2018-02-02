package jce;

import static jce.properties.TextProperty.SOURCE_FOLDER;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import jce.properties.EcorificationProperties;

/**
 * Utility class for analyzing source folders of Java projects.
 * @author Timur Saglam
 */
public final class SourceFolderAnalyzer {
    private static final Logger logger = LogManager.getLogger(SourceFolderAnalyzer.class.getName());
    private static final String MAVEN_SRC = "src/main/java";
    private static final String SRC = "src";

    private SourceFolderAnalyzer() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Verifies whether the source folder specified in the {@link EcorificationProperties} (which means in the user
     * property file) exists in the {@link IProject}. If that is not the case this method will try to find another
     * suitable option.
     * @param project is the {@link IProject}.
     * @param properties are the {@link EcorificationProperties}.
     */
    public static void verify(IProject project, EcorificationProperties properties) {
        check(project);
        try {
            List<IPackageFragmentRoot> sourceFolders = getSourceFolders(JavaCore.create(project));
            if (isPropertyValid(properties, sourceFolders)) {
                logger.info("Chose the source folder which specified in the user property file.");
            } else {
                findSourceFolder(sourceFolders, properties);
                logger.warn("Could not find source folder from user property file, used " + properties.get(SOURCE_FOLDER) + " instead.");
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }

    }

    /**
     * Checks whether a specific {@link IProject} is valid (neither null nor nonexistent)
     * @param project is the specific {@link IProject}.
     */
    private static void check(IProject project) {
        if (project == null) {
            throw new IllegalArgumentException("Project can't be null!");
        } else if (!project.exists()) {
            throw new IllegalArgumentException("Project " + project.toString() + "does not exist!");
        }
    }

    /**
     * Tries to find suitable source folder and adapt the {@link EcorificationProperties} according to that source
     * folder. It will choose "src" and "src/main/java" if they exist. If that is not the case the first source folder
     * will be chosen.
     */
    private static void findSourceFolder(List<IPackageFragmentRoot> sourceFolders, EcorificationProperties properties) {
        for (IPackageFragmentRoot folder : sourceFolders) { // if a normal source folder can be found:
            if (fullName(folder).equals(SRC) || fullName(folder).equals(MAVEN_SRC)) {
                properties.set(SOURCE_FOLDER, fullName(folder)); // use the normal one.
                return;
            }
        } // else, use the first source folder that was found:
        properties.set(SOURCE_FOLDER, fullName(sourceFolders.get(0)));
    }

    /**
     * Generates the full name of a source folder which is an {@link IPackageFragmentRoot}.
     */
    private static String fullName(IPackageFragmentRoot sourceFolder) {
        return sourceFolder.getPath().removeFirstSegments(1).toString();
    }

    /**
     * Returns all source folders of a {@link IJavaProject} in a list.
     */
    private static List<IPackageFragmentRoot> getSourceFolders(IJavaProject project) throws JavaModelException {
        List<IPackageFragmentRoot> sourceFolders = new LinkedList<>();
        for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) { // for every IPackageFragmentRoot:
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) { // if root is source folder
                sourceFolders.add(root);
            }
        }
        return sourceFolders;
    }

    /**
     * Checks whether the source folder specified in the user property file exists.
     */
    private static boolean isPropertyValid(EcorificationProperties properties, List<IPackageFragmentRoot> sourceFolders) {
        for (IPackageFragmentRoot folder : sourceFolders) { // try to find the specified folder:
            if (fullName(folder).equals(properties.get(SOURCE_FOLDER))) {
                return true; // return true if found.
            }
        }
        return false; // source folder does not exist.
    }
}
