package jce.generators;

import static jce.properties.TextProperty.SOURCE_FOLDER;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import eme.generator.GeneratedEcoreMetamodel;
import eme.generator.saving.SavingInformation;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.PathHelper;
import jce.util.ResourceRefresher;

/**
 * Factory class for the creation of generation models (see {@link GenModel}) from Ecore metamodels. This allows also
 * makes the generation models persistent.
 * @author Timur Saglam
 */
public class GenModelGenerator {
    private static final Logger logger = LogManager.getLogger(GenModelGenerator.class.getName());
    private static final char SYSTEM_DEPENDENT_SEPARATOR = File.separatorChar;
    private static final char SYSTEM_INDEPENDENT_SEPARATOR = '/';
    private final GenJDKLevel complianceLevel;
    private final String importerID;
    private final EcorificationProperties properties;
    private final String rootExtendsClass;
    private final String xmlEncoding;

    /**
     * Basic constructor builds GenModelGenerator with default values: JDK Level 8.0, root super class
     * {@link MinimalEObjectImpl.Container}, UTF8 XML encoding and the default importer ID.
     * @param properties are the ecorification properties.
     */
    public GenModelGenerator(EcorificationProperties properties) {
        this.properties = properties;
        complianceLevel = GenJDKLevel.JDK80_LITERAL;
        importerID = "org.eclipse.emf.importer.ecore";
        rootExtendsClass = "org.eclipse.emf.ecore.impl.MinimalEObjectImpl$Container";
        xmlEncoding = "UTF-8";
    }

    /**
     * Constructor that builds the GenModelGenerator with custom values.
     * @param properties are the ecorification properties.
     * @param complianceLevel is the JDK compliance level (see {@link GenJDKLevel}).
     * @param importerID is the the value of the 'Importer ID' attribute. Default is "org.eclipse.emf.importer.ecore".
     * @param rootExtendsClass is the the name of the class which is extended by the roots.
     * @param xmlEncoding is the XML encoding (e.g. UTF-8 or ASCII).
     */
    public GenModelGenerator(EcorificationProperties properties, GenJDKLevel complianceLevel, String importerID, String rootExtendsClass,
            String xmlEncoding) {
        this.properties = properties;
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
            PathHelper pathHelper = new PathHelper(SYSTEM_DEPENDENT_SEPARATOR);
            SavingInformation information = metamodel.getSavingInformation();
            String modelName = information.getFileName();
            String modelPath = information.getFilePath();
            String projectName = SYSTEM_INDEPENDENT_SEPARATOR + pathHelper.getLastSegment(pathHelper.cutLastSegment(modelPath));
            GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
            genModel.setModelDirectory(projectName + SYSTEM_INDEPENDENT_SEPARATOR + properties.get(SOURCE_FOLDER));
            genModel.setModelPluginID(projectName.substring(1));
            genModel.setModelName(modelName);
            genModel.setRootExtendsClass(rootExtendsClass);
            genModel.setImporterID(importerID);
            genModel.setComplianceLevel(complianceLevel);
            genModel.setOperationReflection(true);
            genModel.setImportOrganizing(true);
            genModel.getForeignModel().add(modelName + ".ecore");
            genModel.initialize(Collections.singleton(metamodel.getRoot()));
            GenPackage rootPackage = genModel.getGenPackages().get(0);
            if (rootPackage != null) {
            	rootPackage.setFileExtensions(properties.get(TextProperty.MODEL_FILE_EXTENSION));
            }
            URI uri = saveGenModel(genModel, modelPath, modelName); // IMPORTANT: first save the GenModel
            return loadGenModel(uri); // and then LOAD IT AGAIN (prevents package URI exception)!
        }
        throw new IllegalArgumentException("Can create GenModel only from saved metamodels!");
    }

    /**
     * Loads and returns a GenModel from a specific URI.
     */
    private GenModel loadGenModel(URI uri) {
        ResourceSet resourceSet = new ResourceSetImpl();
        Map<String, Object> extensionMap = resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
        extensionMap.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new EcoreResourceFactoryImpl());
        resourceSet.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
        EObject object = resourceSet.getResource(uri, true).getContents().get(0);
        if (object instanceof GenModel) {
            return (GenModel) object;
        }
        throw new IllegalArgumentException("URI does not lead to a GenModel!");
    }

    /**
     * Saves a GenModel as a file and refreshes the output folder. Returns the URI of the file.
     */
    private URI saveGenModel(GenModel genModel, String modelPath, String modelName) {
        URI genModelURI = URI.createFileURI(modelPath + modelName + ".genmodel");
        try {
            final XMIResourceImpl genModelResource = new XMIResourceImpl(genModelURI);
            genModelResource.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, xmlEncoding);
            genModelResource.getContents().add(genModel);
            genModelResource.save(Collections.EMPTY_MAP);
            ResourceRefresher.refresh(modelPath);
        } catch (IOException exception) {
            logger.error("Error while saving the generator model: ", exception);
        }
        logger.info("The genmodel was saved under: " + modelPath);
        return genModelURI;
    }
}