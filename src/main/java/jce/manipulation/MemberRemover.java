package jce.manipulation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class MemberRemover extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the package names.
     * @param ecorePackage is the name of the Ecore code base package.
     * @param wrapperPackage is the name of the wrapper code base package.
     */
    public MemberRemover(String ecorePackage, String wrapperPackage) {
        super(ecorePackage, wrapperPackage);
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