package jce.codegen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import jce.util.ProgressMonitorAdapter;

/**
 * Helper class that edits an project do add the Xtend dependencies.
 * @author Timur Saglam
 */
public final class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());
    private static final char SLASH = File.separatorChar;
    private static final String XTEND = "xtend-gen"; // Xtend folder name

    private XtendLibraryHelper() {
        // private constructor.
    }

    /**
     * Adds the Xtend dependencies to a project and creates the xtend-gen source folder.
     * @param javaProject is the {@link IJavaProject} instance of the project.
     */
    public static void addXtendLibs(IProject project) {
        logger.info("Adding Xtend dependencies...");
        createXtendFolder(project);
        addClasspathEntry(project);
        addBuildProperty(project);
        addManifestEntries(project);
    }

    /**
     * Adds the Xtend folder (xtend-gen) to the build.properties file.
     */
    private static void addBuildProperty(IProject project) {
        try {
            IPluginModelBase base = PluginRegistry.findModel(project);
            IBuildModel buildModel = PluginRegistry.createBuildModel(base);
            IBuildEntry entry = buildModel.getBuild().getEntry("source..");
            entry.addToken(XTEND + SLASH); // TODO (MEDIUM) check if duplicate
            if (buildModel instanceof IEditableModel) { // if saveable
                ((IEditableModel) buildModel).save(); // save changes
            }
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }

    /**
     * Retrieves the class path file from the {@link IJavaProject}, adds an {@link IClasspathEntry} for the xtend-gen
     * source folder and sets the changed content.
     */
    private static void addClasspathEntry(IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
            System.arraycopy(entries, 0, newEntries, 0, entries.length);
            String xtendDirectory = SLASH + javaProject.getElementName() + SLASH + XTEND;
            newEntries[entries.length] = JavaCore.newSourceEntry(new org.eclipse.core.runtime.Path(xtendDirectory));
            javaProject.setRawClasspath(newEntries, new NullProgressMonitor()); // TODO (MEDIUM) check if duplicate
        } catch (JavaModelException exception) {
            logger.error(exception);
        }
    }

    /**
     * Adds Xtend manifest entries to the manifest file.
     * @param project is the {@link IJavaProject}.
     */
    private static void addManifestEntries(IProject project) {
        IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation(); // workspace path
        String folder = workspace.toString() + project.getFolder("META-INF").getFullPath(); // manifest folder
        File file = new File(folder + SLASH + "MANIFEST.MF"); // manifest file
        if (file.exists()) {
            List<String> manifest = read(file.toPath());
            List<String> newManifest = edit(manifest);
            write(file.toPath(), newManifest);
        } else {
            logger.error("Could not find MANIFEST.MF file in " + folder);
        }
    }

    /**
     * Creates the binary file folder for Xtend. This is the xtend-bin folder.
     */
    private static void createXtendFolder(IProject project) {
        IFolder folder = project.getFolder(XTEND);
        if (!folder.exists()) {
            try {
                folder.create(false, true, new ProgressMonitorAdapter(logger));
            } catch (CoreException exception) {
                logger.fatal(exception);
            }
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
            newManifest.add(line); // TODO (MEDIUM) check if duplicate
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