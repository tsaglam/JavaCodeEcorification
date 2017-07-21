package jce.manipulation;

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

    /**
     * Visits all types of an {@link ICompilationUnit} to encapsulate all fields.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        FieldEncapsulationVisitor visitor = new FieldEncapsulationVisitor();
        CompilationUnit parsedUnit = parse(unit);
        parsedUnit.accept(visitor);
    }
}