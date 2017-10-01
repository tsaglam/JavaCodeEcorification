package jce.handlers;

import org.eclipse.core.resources.IProject;

import eme.handlers.ProjectHandler;
import jce.JavaCodeEcorification;

/**
 * Handler for starting the Java code ecorification.
 * @author Timur Saglam
 */
public class EcorificationHandler extends ProjectHandler {

    /**
     * Basic constructor, sets the message box title.
     */
    public EcorificationHandler() {
        super("JavaCodeEcorification");
    }

    /**
     * @see eme.handlersProjectHandler#startExtraction(org.eclipse.core.resources.IProject)
     */
    @Override
    protected void startExtraction(IProject project) {
        new JavaCodeEcorification().start(project);
    }
}