package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;

import eme.model.IntermediateModel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Changes the inheritance of the origin code to let the original classes
 * inherit from the generated unification classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator extends AbstractCodeManipulator {
    private final IntermediateModel model;

    /**
     * Simple constructor that sets the properties.
     * @param model is the {@link IntermediateModel} needed for the Ecorification
     * scope.
     * @param properties are the {@link EcorificationProperties}.
     */
    public InheritanceManipulator(IntermediateModel model, EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
        this.model = model;
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (model.isTypeSelected(getPackageMemberName(unit))) { // only apply on origin type in scope
            ASTVisitor visitor = new InheritanceManipulationVisitor(unit.getParent().getElementName(), properties);
            ASTUtil.applyVisitorModifications(unit, visitor, monitor);
        }
    }
}