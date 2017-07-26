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
public final class PackageFilter {
    private static final Logger logger = LogManager.getLogger(PackageFilter.class.getName());

    private PackageFilter() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Retrieves all {@link IPackageFragment}s of a specific {@link IProject} that do not start with any prefix of a
     * specific set of prefixes.
     * @param project is the specific {@link IProject}.
     * @param prefixes is the specific set of prefixes.
     * @return a list of {@link IPackageFragment}s that start with a specific prefix.
     */
    public static List<IPackageFragment> startsNotWith(IProject project, String... prefixes) {
        List<IPackageFragment> filteredFragments = new LinkedList<>();
        for (IPackageFragment fragment : retrievePackages(project)) { // get packages
            if (!startsWith(fragment, prefixes)) {
                filteredFragments.add(fragment); // filter packages
            }
        }
        return filteredFragments;
    }

    /**
     * Retrieves all {@link IPackageFragment}s of a specific {@link IProject} that start with at least one of a specific
     * set of prefixes.
     * @param project is the specific {@link IProject}.
     * @param prefixes is the specific set of prefixes.
     * @return a list of {@link IPackageFragment}s that start with a specific prefix.
     */
    public static List<IPackageFragment> startsWith(IProject project, String... prefixes) {
        List<IPackageFragment> filteredFragments = new LinkedList<>();
        for (IPackageFragment fragment : retrievePackages(project)) { // get packages
            if (startsWith(fragment, prefixes)) {
                filteredFragments.add(fragment); // filter packages
            }
        }
        return filteredFragments;
    }

    /**
     * Retrieves every {@link IPackageFragment} from a specific {@link IProject}.
     */
    private static IPackageFragment[] retrievePackages(IProject project) {
        try {
            return JavaCore.create(project).getPackageFragments();
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
        return null;
    }

    /**
     * Checks whether a package fragment starts with at least one of a defined set of prefixes.
     */
    private static boolean startsWith(IPackageFragment fragment, String... prefixes) {
        for (String prefix : prefixes) {
            if (fragment.getElementName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}