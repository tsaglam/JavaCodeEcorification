package jce;

import static jce.properties.TextProperty.ECORE_PACKAGE;
import static jce.properties.TextProperty.PROJECT_SUFFIX;
import static jce.properties.TextProperty.ROOT_CONTAINER;

import eme.EcoreMetamodelExtraction;
import eme.properties.BinaryProperty;
import eme.properties.ExtractionProperties;
import eme.properties.TextProperty;
import jce.properties.EcorificationProperties;

/**
 * Ecore metamodel extraction class for the Java code Ecorification. Extends the {@link EcoreMetamodelExtraction} class
 * to offer a tweaked version of the extraction process.
 * @author Timur Saglam
 */
public class EcorificationExtraction extends EcoreMetamodelExtraction {
    private static final String DATATYPE_PACKAGE = "datatypes";
    private static final String SAVING_STRATEGY = "CopyProject";

    /**
     * Basic constructor. Takes the {@link EcorificationProperties} to configure the extraction.
     * @param properties are the {@link ExtractionProperties}.
     */
    public EcorificationExtraction(EcorificationProperties properties) {
        configure(properties.get(PROJECT_SUFFIX), properties.get(ECORE_PACKAGE), properties.get(ROOT_CONTAINER));
    }

    /**
     * Configures the extraction properties. JCE Properties are referenced directly, EME properties are referenced witht
     * the class name.
     */
    private void configure(String projectSuffix, String defaultPackage, String rootName) {
        getProperties().set(TextProperty.PROJECT_SUFFIX, projectSuffix);
        getProperties().set(TextProperty.DEFAULT_PACKAGE, defaultPackage);
        getProperties().set(TextProperty.ROOT_NAME, rootName);
        getProperties().set(TextProperty.SAVING_STRATEGY, SAVING_STRATEGY);
        getProperties().set(TextProperty.DATATYPE_PACKAGE, DATATYPE_PACKAGE);
        getProperties().set(BinaryProperty.DUMMY_CLASS, false);
        getProperties().set(BinaryProperty.ROOT_CONTAINER, true);
        getProperties().set(BinaryProperty.FINAL_AS_UNCHANGEABLE, false);
        getProperties().set(BinaryProperty.NESTED_TYPES, false);
        getProperties().set(BinaryProperty.PARAMETER_MULTIPLICITIES, false);
    }
}
