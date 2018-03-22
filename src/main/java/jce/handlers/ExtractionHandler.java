package jce.handlers;

import static jce.properties.TextProperty.PROJECT_SUFFIX;

import org.eclipse.core.resources.IProject;

import eme.handlers.ProjectHandler;
import jce.EcorificationExtraction;
import jce.properties.EcorificationProperties;

/**
 * Handler for starting only the Ecore metamodel extraction configured for the Java code ecorification.
 * @author Timur Saglam
 */
public class ExtractionHandler extends ProjectHandler {

    /**
     * Basic constructor, sets the message box title.
     */
    public ExtractionHandler() {
        super("JavaCodeEcorification");
    }

    /**
     * @see eme.handlersProjectHandler#startExtraction(org.eclipse.core.resources.IProject)
     */
    @Override
    protected void startExtraction(IProject project) {
        EcorificationProperties properties = new EcorificationProperties(); // new properties
        properties.set(PROJECT_SUFFIX, properties.get(PROJECT_SUFFIX) + "Model"); // edit the project suffix
        new EcorificationExtraction(properties).extract(project); // start only the ecorification extraction
    }
}