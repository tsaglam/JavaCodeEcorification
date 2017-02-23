package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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
public class InheritanceManipulator {
    private static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());

    /**
     * Changes the inheritance for all classes of specific packages.
     * @param packages are the specific packages.
     * @param project is the {@link IProject} that contains the packages.
     */
    public void manipulate(IPackageFragment[] packages, IProject project) {
        logger.info("Starting the inheritance manipulation...");
        try {
            project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
            for (IPackageFragment mypackage : packages) {
                if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    editTypesIn(mypackage);
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        } catch (CoreException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Visits all types of all {@link ICompilationUnit}s of a {@link IPackageFragment}.
     * @param myPackage is the {@link IPackageFragment}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    private void editTypesIn(IPackageFragment myPackage) throws JavaModelException {
        for (ICompilationUnit unit : myPackage.getCompilationUnits()) {
            TypeVisitor visitor = new TypeVisitor(myPackage.getElementName());
            unit.becomeWorkingCopy(new NullProgressMonitor());
            IDocument document = new Document(unit.getSource());
            CompilationUnit parse = parse(unit);
            parse.recordModifications();
            parse.accept(visitor);
            TextEdit edits = parse.rewrite(document, null);
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

    /**
     * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
     * @param unit is the {@link ICompilationUnit}.
     * @return the {@link ASTNode}.
     */
    private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}