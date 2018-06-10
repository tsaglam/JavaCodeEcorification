package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import eme.model.IntermediateModel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Changes the visibility of default types and default inner classes of the
 * origin code to public to make them visible.
 * @author Timur Saglam
 */
public class ClassExposer extends AbstractCodeManipulator {
    private final IntermediateModel model;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     * @param model is the {@link IntermediateModel} needed for the Ecorification
     * scope.
     */
    public ClassExposer(IntermediateModel model, EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
        this.model = model;
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (properties.get(BinaryProperty.EXPOSE_CLASSES) && model.isTypeSelected(getPackageMemberName(unit))) { // only apply on origin type in scope
            ASTUtil.applyVisitorModifications(unit, new ClassExpositionVisitor(properties), monitor);
        }
    }
}