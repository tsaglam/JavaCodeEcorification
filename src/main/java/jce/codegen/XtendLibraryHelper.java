package jce.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import jce.ProjectDirectories;

/**
 * TODO (MEDIUM) Add comments.
 * @author Timur Saglam
 */
public class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());

    public static void addXtendLibs(IJavaProject project, ProjectDirectories directories) {
        addClasspathEntry(project, directories);
        editManifest(directories);
    }

    private static void addClasspathEntry(IJavaProject project, ProjectDirectories directories) {
        try {
            ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
            entries.add(JavaCore.newSourceEntry(new Path(directories.getProjectDirectory().getAbsolutePath() + File.separator + "xtend-gen")));
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    private static void editManifest(ProjectDirectories directories) {
        // TODO implement editManifest(IJavaProject project)
    }
}