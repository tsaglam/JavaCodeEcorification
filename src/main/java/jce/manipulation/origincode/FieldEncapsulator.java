package jce.manipulation.origincode;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import jce.properties.EcorificationProperties;

/**
 * Encapsulates the fields of the origin code. This is necessary for the removal of the fields.
 * @author Timur Saglam
 */
public class FieldEncapsulator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FieldEncapsulator(EcorificationProperties properties) {
        super(properties);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        FieldEncapsulationVisitor visitor = new FieldEncapsulationVisitor(properties);
        CompilationUnit parsedUnit = parse(unit);
        parsedUnit.accept(visitor);
    }
}