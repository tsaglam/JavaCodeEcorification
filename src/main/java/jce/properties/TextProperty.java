package jce.properties;

import eme.properties.ITextProperty;

public enum TextProperty implements ITextProperty {
    ECORE_PACKAGE("EcorePackageName", "ecore"),
    WRAPPER_PACKAGE("WrapperPackageName", "unification"),
    WRAPPER_PREFIX("WrapperPrefix", "Unified"),
    WRAPPER_SUFFIX("WrapperSuffix", "");

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
