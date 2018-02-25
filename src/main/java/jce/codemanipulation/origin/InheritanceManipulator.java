package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;

import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Changes the inheritance of the origin code to let the original classes inherit from the generated unification
 * classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator extends AbstractCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public InheritanceManipulator(EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        ASTVisitor visitor = new InheritanceManipulationVisitor(unit.getParent().getElementName(), properties);
        ASTUtil.applyVisitorModifications(unit, visitor, monitor);
    }
}