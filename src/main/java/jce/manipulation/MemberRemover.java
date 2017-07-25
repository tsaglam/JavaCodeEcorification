package jce.manipulation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import eme.generator.GeneratedEcoreMetamodel;
import jce.properties.EcorificationProperties;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class MemberRemover extends OriginCodeManipulator {
    private GeneratedEcoreMetamodel metamodel;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public MemberRemover(EcorificationProperties properties) {
        super(properties);
    }

    /**
     * Setter for the {@link GeneratedEcoreMetamodel}. Has to be set before calling
     * {@link OriginCodeManipulator#manipulate(org.eclipse.core.resources.IProject))}.
     * @param metamodel is the metamodel to set.
     */
    public void setMetamodel(GeneratedEcoreMetamodel metamodel) {
        this.metamodel = metamodel;
    }

    /**
     * Visits all types of an {@link ICompilationUnit} to remove all non-static fields and their access methods.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (metamodel == null) {
            throw new IllegalStateException("Please set the generated Ecore metamodel before calling manipulate().");
        }
        applyVisitorModifications(unit, new MemberRemovalVisitor(metamodel));
    }
}