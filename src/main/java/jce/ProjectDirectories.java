package jce;

import java.io.File;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codegen.PathHelper;

/**
 * Generator and container class for project directories.
 * @author Timur Saglam
 */
public class ProjectDirectories {
    private static final Logger logger = LogManager.getLogger(ProjectDirectories.class.getName());
    private static final String MANIFEST_FOLDER = "META-INF";
    private static final char SLASH = File.separatorChar;
    private final File manifestDirectory;
    private final File modelDirectory;
    private final PathHelper pathHelper;
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
        workspaceDirectory = new File(pathHelper.nthParentOf(metamodel.getSavingInformation().getFilePath(), 3));
        sourceDirectory = new File(workspaceDirectory.getAbsolutePath() + genModel.getModelDirectory());
        modelDirectory = new File(workspaceDirectory.getAbsolutePath() + genModel.getModelProjectDirectory());
        manifestDirectory = new File(workspaceDirectory.getAbsolutePath() + SLASH + MANIFEST_FOLDER);
        if (!validate()) {
            logger.error("Some directories do not exist.");
        }
    }

    /**
     * Accessor for the manifestDirectory.
     * @return the manifestDirectory
     */
    public File getManifestDirectory() {
        return manifestDirectory;
    }

    /**
     * Accessor for the modelDirectory.
     * @return the modelDirectory
     */
    public File getModelDirectory() {
        return modelDirectory;
    }

    /**
     * Accessor for the sourceDirectory.
     * @return the sourceDirectory
     */
    public File getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Accessor for the workspaceDirectory.
     * @return the workspaceDirectory
     */
    public File getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    /**
     * Validates whether all paths exist or not.
     * @return true if all paths exist.
     * @see File#exists()
     */
    public boolean validate() {
        return workspaceDirectory.exists() && sourceDirectory.exists() && modelDirectory.exists() && manifestDirectory.exists();
    }
}