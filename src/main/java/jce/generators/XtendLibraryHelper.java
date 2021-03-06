package jce.generators;

import static jce.properties.TextProperty.SOURCE_FOLDER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import jce.properties.EcorificationProperties;
import jce.util.PathHelper;
import jce.util.ResourceRefresher;
import jce.util.logging.MonitorFactory;

/**
 * Helper class that edits an project do add the Xtend dependencies.
 * @author Timur Saglam
 */
public final class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());
    private static final char SLASH = File.separatorChar;
    private static final String XTEND = "xtend-gen"; // Xtend folder name

    private XtendLibraryHelper() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Adds the Xtend dependencies to a project and creates the xtend-gen source folder.
     * @param project is the {@link IProject} instance of the project.
     * @param properties are the Ecorification properties.
     */
    public static void addXtendLibs(IProject project, EcorificationProperties properties) {
        logger.info("Adding Xtend dependencies...");
        IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties);
        ResourceRefresher.refresh(project);
        PathHelper path = new PathHelper(SLASH);
        String xtendDirectory = path.append(path.getParent(properties.get(SOURCE_FOLDER)), XTEND);
        createXtendFolder(project, xtendDirectory, monitor);
        addClasspathEntry(project, xtendDirectory, monitor);
        addBuildProperty(project, xtendDirectory);
        addManifestEntries(project);
        updateProjectDescription(project, monitor);
    }

    /**
     * Adds the Xtend folder (xtend-gen) to the build.properties file.
     */
    private static void addBuildProperty(IProject project, String xtendDirectory) {
        try {
            IPluginModelBase base = PluginRegistry.findModel(project);
            if (base != null) {
                IBuildModel buildModel = PluginRegistry.createBuildModel(base);
                IBuildEntry entry = buildModel.getBuild().getEntry("source..");
                String token = xtendDirectory + SLASH;
                if (entry.contains(token)) {
                    logger.warn("build.properties already contains " + token);
                } else {
                    entry.addToken(token);
                }
                if (buildModel instanceof IEditableModel) { // if saveable
                    ((IEditableModel) buildModel).save(); // save changes
                }
            } else {
                logger.error("Generated project is no plug-in project or contains a malformed manifest.");
            }
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }

    /**
     * Retrieves the class path file from the {@link IProject}, adds an {@link IClasspathEntry} for the xtend-gen source
     * folder and sets the changed content.
     */
    private static void addClasspathEntry(IProject project, String xtendDirectory, IProgressMonitor monitor) {
        IJavaProject javaProject = JavaCore.create(project);
        IPath path = javaProject.getPath().append(xtendDirectory); // Path to xtend folder
        IClasspathEntry newEntry = JavaCore.newSourceEntry(path); // Entry for the .classpath file
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            if (Arrays.asList(entries).contains(newEntry)) {
                logger.warn(".classpath already contains " + xtendDirectory);
            } else {
                entries = extendArray(entries, new IClasspathEntry[entries.length + 1], newEntry);
                javaProject.setRawClasspath(entries, monitor);
            }
        } catch (JavaModelException exception) {
            logger.error(exception);
        }
    }

    /**
     * Adds Xtend manifest entries to the manifest file.
     * @param project is the {@link IProject}.
     */
    private static void addManifestEntries(IProject project) {
        IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation(); // workspace path
        String folder = workspace.toString() + project.getFolder("META-INF").getFullPath(); // manifest folder
        File file = new File(folder + SLASH + "MANIFEST.MF"); // manifest file
        if (file.exists()) {
            List<String> manifest = readFile(file.toPath());
            List<String> newManifest = editManifest(manifest);
            writeFile(file.toPath(), newManifest);
        } else {
            logger.error("Could not find MANIFEST.MF file in " + folder);
        }
    }

    /**
     * Creates the binary file folder for Xtend. This is the xtend-bin folder.
     */
    private static void createXtendFolder(IProject project, String folderPath, IProgressMonitor monitor) {
        IFolder folder = project.getFolder(folderPath);
        if (!folder.exists()) {
            try {
                folder.create(false, true, monitor);
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
    private static List<String> editManifest(List<String> manifest) {
        List<String> newManifest = new LinkedList<String>();
        for (String line : manifest) {
            newManifest.add(line);
            if (line.contains("Require-Bundle:")) {
                newManifest.add(" com.google.guava,");
                newManifest.add(" org.eclipse.xtext.xbase.lib,");
                newManifest.add(" org.eclipse.xtend.lib,");
                newManifest.add(" org.eclipse.xtend.lib.macro,");
                newManifest.add(" edu.kit.ipd.sdq.activextendannotations,");
            }
        }
        return newManifest;
    }

    /**
     * Extends an array by copying all the elements of the old array to a new array and adding the new element in the
     * last position. If the array already contains the new element the original array is returned. Because it is not
     * able to dynamically create generic arrays in Java, the new array has to be passes as a parameter. It should have
     * the size of the old arrays plus one.
     */
    private static <T> T[] extendArray(T[] oldArray, T[] newArray, T element) {
        if (Arrays.asList(oldArray).contains(element)) {
            logger.warn(".project already contains " + element.toString());
            return oldArray; // array is not changed.
        } else {
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            newArray[oldArray.length] = element;
            return newArray;
        }
    }

    /**
     * Reads a file from a path and return its content.
     */
    private static List<String> readFile(Path path) {
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
     * Adds the xtext nature and builder command to the .project file of the project.
     */
    private static void updateProjectDescription(IProject project, IProgressMonitor monitor) {
        String builderName = "org.eclipse.xtext.ui.shared.xtextBuilder";
        String xtextNature = "org.eclipse.xtext.ui.shared.xtextNature";
        try {
            IProjectDescription description = project.getDescription();
            // add xtext builder:
            ICommand[] commands = description.getBuildSpec();
            ICommand command = description.newCommand();
            command.setBuilderName(builderName);
            description.setBuildSpec(extendArray(commands, new ICommand[commands.length + 1], command));
            // Add xtext nature:
            String[] natures = description.getNatureIds();
            description.setNatureIds(extendArray(natures, new String[natures.length + 1], xtextNature));
            // Set updated description:
            project.setDescription(description, monitor);
        } catch (CoreException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Writes in file at a specific path from a list of lines.
     */
    private static void writeFile(Path path, List<String> content) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String line : content) {
                writer.write(line + System.getProperty("line.separator"));
            }
        } catch (IOException exception) {
            logger.error(exception);
        }
    }
}