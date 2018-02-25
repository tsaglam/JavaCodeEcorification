package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Changes the visibility of default types and default inner classes of the origin code to public to make them visible.
 * @author Timur Saglam
 */
public class ClassExposer extends AbstractCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public ClassExposer(EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (properties.get(BinaryProperty.EXPOSE_CLASSES)) {
            ASTUtil.applyVisitorModifications(unit, new ClassExpositionVisitor(properties), monitor);
        }
    }
}