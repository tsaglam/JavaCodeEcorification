package jce.codegen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import jce.util.ProgressMonitorAdapter;
import jce.util.ProjectDirectories;

/**
 * Helper class that edits an project do add the Xtend dependencies.
 * @author Timur Saglam
 */
public final class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());
    private static final char SLASH = File.separatorChar;

    private XtendLibraryHelper() {
        // private constructor.
    }

    /**
     * Adds the Xtend dependencies to a project and creates the xtend-gen source folder.
     * @param project is the {@link IJavaProject} instance of the project.
     * @param directories is the {@link ProjectDirectories} instance of the project.
     */
    public static void addXtendLibs(IJavaProject project, ProjectDirectories directories) {
        createXtendFolder(project);
        addClasspathEntry(project); // TODO (MEDIUM) add xtend-gen folder to build.properties file.
        addManifestEntries(directories);
    }

    /**
     * Retrieves the class path file from the {@link IJavaProject}, adds an {@link IClasspathEntry} for the xtend-gen
     * source folder and sets the changed content.
     * @param project is the {@link IJavaProject}.
     */
    private static void addClasspathEntry(IJavaProject project) {
        try {
            ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
            String xtendDirectory = SLASH + project.getElementName() + SLASH + "xtend-gen";
            entries.add(JavaCore.newSourceEntry(new org.eclipse.core.runtime.Path(xtendDirectory)));
            IClasspathEntry[] entryArray = new IClasspathEntry[entries.size()];
            project.setRawClasspath(entries.toArray(entryArray), null);
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Adds Xtend manifest entries to the manifest file.
     * @param directories is the {@link ProjectDirectories} instance of the project.
     */
    private static void addManifestEntries(ProjectDirectories directories) {
        File file = new File(directories.getManifestDirectory() + SLASH + "MANIFEST.MF");
        if (file.exists()) {
            List<String> manifest = read(file.toPath());
            List<String> newManifest = edit(manifest);
            write(file.toPath(), newManifest);
        } else {
            logger.error("Could not find MANIFEST.MF file in " + directories.getManifestDirectory());
        }
    }

    /**
     * Creates the binary file folder for Xtend. This is the xtend-bin folder.
     */
    private static void createXtendFolder(IJavaProject project) {
        IFolder folder = project.getProject().getFolder("xtend-gen");
        try {
            folder.create(false, true, new ProgressMonitorAdapter(logger));
        } catch (CoreException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Edits a manifest file in form of a list of manifest entries. Returns a new manifest file list.
     * @param manifest is the original manifest file list.
     * @return the edited manifest file list.
     */
    private static List<String> edit(List<String> manifest) {
        List<String> newManifest = new LinkedList<String>();
        for (String line : manifest) {
            newManifest.add(line);
            if (line.contains("Require-Bundle:")) {
                newManifest.add(" com.google.guava,");
                newManifest.add(" org.eclipse.xtext.xbase.lib,");
                newManifest.add(" org.eclipse.xtend.lib,");
                newManifest.add(" org.eclipse.xtend.lib.macro,");
            }
        }
        return newManifest;
    }

    /**
     * Reads a file from a path and return its content.
     * @param path is the path.
     * @return the content as a list of lines.
     */
    private static List<String> read(Path path) {
        List<String> content = new LinkedList<String>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (IOException exception) {
            logger.error(exception);
        }
        return content;
    }

    /**
     * Writes in file at a specific path from a list of lines.
     * @param path is the path of the file.
     * @param content is the list of lines.
     */
    private static void write(Path path, List<String> content) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String line : content) {
                writer.write(line + System.getProperty("line.separator"));
            }
        } catch (IOException exception) {
            logger.error(exception);
        }
    }
}