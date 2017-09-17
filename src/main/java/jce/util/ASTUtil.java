package jce.util;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import jce.properties.EcorificationProperties;

public final class ASTUtil {
    private static final Logger logger = LogManager.getLogger(ASTUtil.class.getName());

    private ASTUtil() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Applies all recorded changes of an {@link ImportRewrite} to an {@link ICompilationUnit}.
     * @param unit is the {@link ICompilationUnit}.
     * @param importRewrite is the {@link ImportRewrite}.
     */
    public static void applyImportRewrite(ICompilationUnit unit, ImportRewrite importRewrite, IProgressMonitor monitor) {
        if (importRewrite.hasRecordedChanges()) { // apply changes if existing
            logChange(unit, importRewrite, monitor); // log the changed imports
            try {
                TextEdit edits = importRewrite.rewriteImports(monitor); // create text edit
                applyTextEdit(edits, unit, monitor); // apply text edit to compilation unit.
            } catch (MalformedTreeException exception) {
                logger.fatal(exception);
            } catch (CoreException exception) {
                logger.fatal(exception);
            }
        }
    }

    /**
     * Applies an {@link TextEdit} instance to an {@link ICompilationUnit}.
     * @param edits is the {@link TextEdit} instance.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    public static void applyTextEdit(TextEdit edits, ICompilationUnit unit, IProgressMonitor monitor) throws JavaModelException {
        IDocument document = new Document(unit.getSource());
        try {
            edits.apply(document);
        } catch (MalformedTreeException exception) {
            logger.fatal(exception);
        } catch (BadLocationException exception) {
            logger.fatal(exception);
        }
        unit.getBuffer().setContents(document.get());
        unit.commitWorkingCopy(true, monitor);
    }

    /**
     * Visits all types of any {@link ICompilationUnit} of a {@link IPackageFragment} with a specific {@link ASTVisitor}
     * and applies all recorded modifications to the Java files.
     * @param unit is the {@link ICompilationUnit}.
     * @param visitor is the specific {@link ASTVisitor}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    public static void applyVisitorModifications(ICompilationUnit unit, ASTVisitor visitor, IProgressMonitor monitor) throws JavaModelException {
        CompilationUnit parsedUnit = parse(unit, monitor);
        parsedUnit.recordModifications();
        parsedUnit.accept(visitor);
        TextEdit edits = parsedUnit.rewrite(new Document(unit.getSource()), null);
        applyTextEdit(edits, unit, monitor);
    }

    /**
     * Logs the changed import if full logging is enabled in the {@link EcorificationProperties}.
     */
    private static void logChange(ICompilationUnit unit, ImportRewrite rewrite, IProgressMonitor monitor) {
        monitor.beginTask(unit.getElementName() + ": removed " + Arrays.toString(rewrite.getRemovedImports()) + ", added "
                + Arrays.toString(rewrite.getAddedImports()), 0);
    }

    /**
     * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
     * @param unit is the {@link ICompilationUnit}.
     * @param monitor is the {@link IProgressMonitor}.
     * @return the {@link ASTNode}.
     * @throws JavaModelException if there is problem with the Java model.
     */
    public static CompilationUnit parse(ICompilationUnit unit, IProgressMonitor monitor) throws JavaModelException {
        unit.becomeWorkingCopy(monitor);
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(monitor); // parse
    }
}
