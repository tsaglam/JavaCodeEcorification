package jce.properties;

import eme.properties.IBinaryProperty;

public enum BinaryProperty implements IBinaryProperty {
    ;

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
