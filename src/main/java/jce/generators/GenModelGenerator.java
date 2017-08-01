package jce.generators;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import eme.generator.GeneratedEcoreMetamodel;
import eme.generator.saving.SavingInformation;
import jce.util.PathHelper;
import jce.util.ResourceRefresher;

/**
 * Creates generation models from Ecore metamodels.
 * @author Timur Saglam
 */
public class GenModelGenerator {
    private static final Logger logger = LogManager.getLogger(GenModelGenerator.class.getName());
    private static final char SLASH = File.separatorChar;
    private final GenJDKLevel complianceLevel;
    private final String importerID;
    private final String rootExtendsClass;
    private final String xmlEncoding;

    /**
     * Basic constructor builds GenModelGenerator with default values.
     */
    public GenModelGenerator() {
        complianceLevel = GenJDKLevel.JDK80_LITERAL;
        importerID = "org.eclipse.emf.importer.ecore";
        rootExtendsClass = "org.eclipse.emf.ecore.impl.MinimalEObjectImpl$Container";
        xmlEncoding = "UTF-8";
    }

    /**
     * Constructor that builds the GenModelGenerator with custom values.
     * @param complianceLevel is the compliance level (see {@link GenJDKLevel})
     * @param importerID is the the new value of the 'Importer ID' attribute.
     * @param rootExtendsClass is the value of the 'Root Extends Class' attribute.
     * @param xmlEncoding is the XML encoding (e.g. UTF-8 or ASCII)
     */
    public GenModelGenerator(GenJDKLevel complianceLevel, String importerID, String rootExtendsClass, String xmlEncoding) {
        this.complianceLevel = complianceLevel;
        this.importerID = importerID;
        this.rootExtendsClass = rootExtendsClass;
        this.xmlEncoding = xmlEncoding;
    }

    /**
     * Generates a generator model for a Ecore metamodel and saves it in the same folder as the metamodel.
     * @param metamodel is the Ecore metamodel, passed through its root package.
     * @return the generator model, a GenModel object.
     */
    public GenModel generate(GeneratedEcoreMetamodel metamodel) {
        if (metamodel.isSaved()) {
            PathHelper pathHelper = new PathHelper(SLASH);
            SavingInformation information = metamodel.getSavingInformation();
            String modelName = information.getFileName();
            String modelPath = information.getFilePath();
            String projectName = SLASH + pathHelper.getLastSegment(pathHelper.cutLastSegment(modelPath));
            GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
            genModel.setModelDirectory(projectName + "/src");
            genModel.setModelPluginID(projectName.substring(1));
            genModel.setModelName(modelName);
            genModel.setRootExtendsClass(rootExtendsClass);
            genModel.setImporterID(importerID);
            genModel.setComplianceLevel(complianceLevel);
            genModel.setOperationReflection(true);
            genModel.setImportOrganizing(true);
            genModel.getForeignModel().add(modelName + ".ecore");
            genModel.initialize(Collections.singleton(metamodel.getRoot()));
            saveGenModel(genModel, modelPath, modelName);
            return genModel;
        }
        throw new IllegalArgumentException("Can create GenModel only from saved metamodels!");
    }

    /**
     * Saves a GenModel as a file and refreshes the output folder.
     */
    private void saveGenModel(GenModel genModel, String modelPath, String modelName) {
        try {
            URI genModelURI = URI.createFileURI(modelPath + modelName + ".genmodel");
            final XMIResourceImpl genModelResource = new XMIResourceImpl(genModelURI);
            genModelResource.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, xmlEncoding);
            genModelResource.getContents().add(genModel);
            genModelResource.save(Collections.EMPTY_MAP);
            ResourceRefresher.refresh(modelPath);
        } catch (IOException exception) {
            logger.error("Error while saving the generator model: ", exception);
        }
        logger.info("The genmodel was saved under: " + modelPath);
    }
}