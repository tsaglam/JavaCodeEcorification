package jce.handlers;

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
        new EcorificationExtraction(new EcorificationProperties()).extract(project);
    }
}