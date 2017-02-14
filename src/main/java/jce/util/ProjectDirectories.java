package jce.util;

import java.io.File;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;

import eme.generator.GeneratedEcoreMetamodel;

/**
 * Generator and container class for project directories.
 * @author Timur Saglam
 */
public class ProjectDirectories {
    private static final String MANIFEST_FOLDER = "META-INF";
    private static final String MODEL_FOLDER = "model";
    private static final char SLASH = File.separatorChar;
    private final File manifestDirectory;
    private final File modelDirectory;
    private final PathHelper pathHelper;
    private final File projectDirectory;
    private final File sourceDirectory;
    private final File workspaceDirectory;

    /**
     * Basic constructor, builds paths from a {@link GeneratedEcoreMetamodel} and a {@link GenModel}.
     * @param metamodel is the {@link GeneratedEcoreMetamodel}.
     * @param genModel is the {@link GenModel}.
     */
    public ProjectDirectories(GeneratedEcoreMetamodel metamodel, GenModel genModel) {
        if (metamodel == null || genModel == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        } else if (!metamodel.isSaved()) {
            throw new IllegalArgumentException("Metamodel has to be saved.");
        }
        pathHelper = new PathHelper(SLASH);
        projectDirectory = new File(pathHelper.parentOf(metamodel.getSavingInformation().getFilePath()));
        workspaceDirectory = new File(pathHelper.parentOf(projectDirectory.getAbsolutePath()));
        sourceDirectory = new File(getProjectDirectory() + SLASH + pathHelper.nameOf(genModel.getModelDirectory()));
        modelDirectory = new File(getProjectDirectory() + SLASH + MODEL_FOLDER);
        manifestDirectory = new File(getProjectDirectory() + SLASH + MANIFEST_FOLDER);
    }

    /**
     * Accessor for the manifestDirectory.
     * @return the manifestDirectory
     */
    public String getManifestDirectory() {
        return manifestDirectory.getAbsolutePath();
    }

    /**
     * Accessor for the modelDirectory.
     * @return the modelDirectory
     */
    public String getModelDirectory() {
        return modelDirectory.getAbsolutePath();
    }

    /**
     * Accessor for the projectDirectory.
     * @return the projectDirectory
     */
    public String getProjectDirectory() {
        return projectDirectory.getAbsolutePath();
    }

    /**
     * Accessor for the sourceDirectory.
     * @return the sourceDirectory
     */
    public String getSourceDirectory() {
        return sourceDirectory.getAbsolutePath();
    }

    /**
     * Accessor for the workspaceDirectory.
     * @return the workspaceDirectory
     */
    public String getWorkspaceDirectory() {
        return workspaceDirectory.getAbsolutePath();
    }

    /**
     * Validates whether all paths exist or not.
     * @return true if all paths exist.
     * @see File#exists()
     */
    public boolean validate() {
        return workspaceDirectory.exists() && projectDirectory.exists() && sourceDirectory.exists() && modelDirectory.exists()
                && manifestDirectory.exists();
    }
}