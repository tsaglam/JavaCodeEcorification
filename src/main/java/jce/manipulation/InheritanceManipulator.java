package jce.manipulation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the package names.
     * @param ecorePackage is the name of the Ecore code base package.
     * @param wrapperPackage is the name of the wrapper code base package.
     */
    public InheritanceManipulator(String ecorePackage, String wrapperPackage) {
        super(ecorePackage, wrapperPackage);
    }

    /**
     * Visits all types of an {@link ICompilationUnit} to override the super class declaration of all classes.
     * declarations.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        applyVisitorModifications(unit, new InheritanceManipulationVisitor(unit.getParent().getElementName()));
    }
}