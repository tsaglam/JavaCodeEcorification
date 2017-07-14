package jce.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Utility class that allows to retrieve a specific packages of an Eclipse Java project.
 * @author Timur Saglam
 */
public class PackageFilter {
    private static final Logger logger = LogManager.getLogger(PackageFilter.class.getName());

    private PackageFilter() {
        // private constructor.
    }

    /**
     * Retrieves all {@link IPackageFragment}s of a specific {@link IProject} that start with a specific prefix.
     * @param project is the specific {@link IProject}.
     * @param prefix is the prefix {@link String}.
     * @return a list of {@link IPackageFragment}s that start with a specific prefix.
     */
    public static List<IPackageFragment> startsWith(IProject project, String prefix) {
        List<IPackageFragment> filteredFragments = new LinkedList<>();
        try {
            for (IPackageFragment fragment : JavaCore.create(project).getPackageFragments()) { // get packages
                if (fragment.getElementName().startsWith(prefix)) {
                    filteredFragments.add(fragment); // filter packages
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
        return filteredFragments;
    }
}