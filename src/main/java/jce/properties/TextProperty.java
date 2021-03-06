package jce.properties;

import eme.properties.ITextProperty;

/**
 * Text properties that can be accessed in the Extraction properties.
 * @author Timur Saglam
 */
public enum TextProperty implements ITextProperty {
    ECORE_PACKAGE("EcorePackageName", "ecore"),
    WRAPPER_PACKAGE("WrapperPackageName", "unification"),
    WRAPPER_PREFIX("WrapperPrefix", "Unified"),
    WRAPPER_SUFFIX("WrapperSuffix", ""),
    FACTORY_PACKAGE("FactoryPackageName", "original"),
    PROJECT_SUFFIX("ProjectSuffix", "Ecorified"),
    SOURCE_FOLDER("SourceFolder", "src"),
    ROOT_CONTAINER("RootContainerName", "RootContainer"),
    FACTORY_SUFFIX("OriginalFactorySuffix", "Old"),
    MODEL_FILE_EXTENSION("ModelFileExtension", "ecorified");
	
    private final String defaultValue;
    private final String key;

    /**
     * Private constructor for enum values with key and default value of an ecorification property.
     * @param key is the key of the property.
     * @param defaultValue is the default value of the property.
     */
    TextProperty(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getKey() {
        return key;
    }

}
