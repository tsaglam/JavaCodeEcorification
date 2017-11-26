package jce.properties;

import eme.properties.IBinaryProperty;

/**
 * Binary properties that can be accessed in the Extraction properties.
 * @author Timur Saglam
 */
public enum BinaryProperty implements IBinaryProperty {
    FULL_LOGGING("FullLogging", false),
    EXPOSE_CLASSES("ExposeClasses", true);
    private final boolean defaultValue;
    private final String key;

    /**
     * Private constructor for enum values with key and default value of an ecorification property.
     * @param key is the key of the property.
     * @param defaultValue is the default value of the property.
     */
    BinaryProperty(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getKey() {
        return key;
    }

}
