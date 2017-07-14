package jce.manipulation;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Encapsulates the fields of the origin code. This is necessary for the removal of the fields.
 * @author Timur Saglam
 */
public class FieldEncapsulator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the package names.
     * @param ecorePackageName is the name of the Ecore code base package.
     * @param wrapperPackageName is the name of the wrapper code base package.
     */
    public FieldEncapsulator(String ecorePackage, String wrapperPackage) {
        super(ecorePackage, wrapperPackage);
    }

    protected void manipulate(IPackageFragment fragment) throws JavaModelException {
        for (ICompilationUnit unit : fragment.getCompilationUnits()) {
            FieldEncapsulationVisitor visitor = new FieldEncapsulationVisitor();
            unit.becomeWorkingCopy(new NullProgressMonitor());
            CompilationUnit parsedUnit = parse(unit);
            parsedUnit.accept(visitor);
        }
    }
}