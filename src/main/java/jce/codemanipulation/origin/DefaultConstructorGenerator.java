package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import eme.model.IntermediateModel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * This class generates default constructors for every class that does not
 * support default constructors. This is a unstable fix for the problem that
 * Ecore factories require default constructors to create instances.
 * @author Timur Saglam
 */
public class DefaultConstructorGenerator extends AbstractCodeManipulator {
    private IntermediateModel model;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     * @param model is the {@link IntermediateModel} needed for the Ecorification
     * scope.
     */
    public DefaultConstructorGenerator(EcorificationProperties properties, IntermediateModel model) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
        this.model = model;
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (model.isTypeSelected(getPackageMemberName(unit))) { // only apply on origin type in scope
            ASTUtil.applyVisitorModifications(unit, new ConstructorGenerationVisitor(), monitor);
        }
    }
}