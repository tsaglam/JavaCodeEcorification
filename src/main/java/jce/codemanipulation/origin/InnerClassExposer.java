package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;

/**
 * Changes the visibility of default inner classes of the origin code to public to make them visible.
 * @author Timur Saglam
 */
public class InnerClassExposer extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public InnerClassExposer(EcorificationProperties properties) {
        super(properties);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (properties.get(BinaryProperty.EXPOSE_INNER_CLASSES)) {
            applyVisitorModifications(unit, new ClassExpositionVisitor(properties));
        }
    }
}