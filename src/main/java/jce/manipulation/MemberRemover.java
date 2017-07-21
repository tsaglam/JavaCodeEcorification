package jce.manipulation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import jce.properties.EcorificationProperties;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class MemberRemover extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public MemberRemover(EcorificationProperties properties) {
        super(properties);
    }

    /**
     * Visits all types of an {@link ICompilationUnit} to remove all non-static fields and their access methods.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        applyVisitorModifications(unit, new MemberRemovalVisitor());
    }
}