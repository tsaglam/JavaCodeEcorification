package jce.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Helper class to refresh folders.
 * @author Timur Saglam
 */
public final class FolderRefresher {
    private static final Logger logger = LogManager.getLogger(FolderRefresher.class.getName());

    private FolderRefresher() {
        // private constructor.
    }

    /**
     * Refreshes the project folder of an {@link IProject}.
     * @param project is the {@link IProject}.
     */
    public static void refresh(IProject project) {
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException exception) {
            logger.warn("Could not refresh project folder. Try that manually.", exception);
        }
    }

    /**
     * Refreshes a folder of an {@link IProject}.
     * @param project is the {@link IProject}.
     * @param folderName is the name of the {@link IFolder} to refresh.
     */
    public static void refresh(IProject project, String folderName) {
        IFolder folder = project.getFolder(folderName);
        try {
            folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException exception) {
            logger.warn("Could not refresh folder " + folderName + ". Try that manually.", exception);
        }

    }
}