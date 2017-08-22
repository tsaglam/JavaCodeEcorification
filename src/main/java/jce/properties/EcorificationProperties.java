package jce.properties;

import eme.properties.AbstractProperties;

/**
 * This class manages the ecorification properties in the user.properties file.
 * @author Timur Saglam
 */
public class EcorificationProperties extends AbstractProperties<TextProperty, BinaryProperty> {

    /**
     * Basic constructor.
     */
    public EcorificationProperties() {
        super("user.properties", "Use this file to configure the Java code ecorification.", "JavaCodeEcorification");
    }
}
