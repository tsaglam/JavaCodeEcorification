package jce.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Helper class to refresh folders.
 * @author Timur Saglam
 */
public final class ResourceRefresher {
    private static final Logger logger = LogManager.getLogger(ResourceRefresher.class.getName());

    private ResourceRefresher() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Refreshes a specific {@link IResource}.
     * @param resource is the {@link IResource}.
     */
    public static void refresh(IResource resource) { // TODO (HIGH) add full logging
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException exception) {
            logger.warn("Could not refresh. Try refreshing manually!", exception);
        }
    }

    /**
     * Refreshes a folder of an {@link IProject}.
     * @param project is the {@link IProject}.
     * @param folderName is the name of the {@link IFolder} to refresh.
     */
    public static void refresh(IProject project, String folderName) {
        refresh(project.getFolder(folderName));
    }

    /**
     * Refreshes the folder of a path.
     * @param folderPath is the path of a folder.
     */
    public static void refresh(String folderPath) {
        Path path = new Path(folderPath);
        if (path.toFile().exists()) {
            refresh(ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(path));
        } else {
            throw new IllegalArgumentException("Path does not exist: " + folderPath);
        }
    }
}