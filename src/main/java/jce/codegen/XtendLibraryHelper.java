package jce.codegen;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * TODO (MEDIUM) Add comments.
 * @author Timur Saglam
 */
public class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());

    public static void addXtendLibs(IJavaProject project) {
        addClasspathEntry(project);
        editManifest(project);
    }

    private static void addClasspathEntry(IJavaProject project) {
        try {
            ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
            entries.add(JavaCore.newSourceEntry(new Path("xtend-gen"))); // TODO (HIGH) use absolute path
            project.setRawClasspath(entries.toArray(new IClasspathEntry[] {}), null);
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    private static void editManifest(IJavaProject project) {
        // TODO implement editManifest(IJavaProject project)
    }
}