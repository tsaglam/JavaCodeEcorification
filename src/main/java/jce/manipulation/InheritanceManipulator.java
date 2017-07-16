package jce.manipulation;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the package names.
     * @param ecorePackageName is the name of the Ecore code base package.
     * @param wrapperPackageName is the name of the wrapper code base package.
     */
    public InheritanceManipulator(String ecorePackage, String wrapperPackage) {
        super(ecorePackage, wrapperPackage);
    }

    /**
     * Visits all types of all {@link ICompilationUnit}s of a {@link IPackageFragment} to override the super class
     * declarations.
     * @param fragment is the {@link IPackageFragment}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    @Override
    protected void manipulate(IPackageFragment fragment) throws JavaModelException {
        for (ICompilationUnit unit : fragment.getCompilationUnits()) {
            InheritanceManipulationVisitor visitor = new InheritanceManipulationVisitor(fragment.getElementName());
            CompilationUnit parsedUnit = parse(unit);
            IDocument document = new Document(unit.getSource());
            parsedUnit.recordModifications();
            parsedUnit.accept(visitor);
            TextEdit edits = parsedUnit.rewrite(document, null);
            try {
                edits.apply(document);
            } catch (MalformedTreeException exception) {
                logger.fatal(exception);
            } catch (BadLocationException exception) {
                logger.fatal(exception);
            }
            unit.getBuffer().setContents(document.get());
            unit.commitWorkingCopy(true, new NullProgressMonitor());
        }
    }
}