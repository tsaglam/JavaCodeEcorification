package jce.util.jdt

import java.util.Arrays
import java.util.Map
import jce.properties.EcorificationProperties
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocument
import org.eclipse.text.edits.MalformedTreeException
import org.eclipse.text.edits.TextEdit

/**
 * Utility class for AST functionality such ass applying import rewrites, visitor modifications and text edits.
 */
final class ASTUtil {
	static final Logger logger = LogManager.getLogger(ASTUtil.getName)

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/** 
	 * Applies all recorded changes of an {@link ImportRewrite} to an {@link ICompilationUnit}.
	 * @param unit is the {@link ICompilationUnit}.
	 * @param importRewrite is the {@link ImportRewrite}.
	 */
	def static void applyImportRewrite(ICompilationUnit unit, ImportRewrite importRewrite, IProgressMonitor monitor) {
		if(importRewrite.hasRecordedChanges) { // apply changes if existing
			logChange(unit, importRewrite, monitor) // log the changed imports
			try {
				var TextEdit edits = importRewrite.rewriteImports(monitor) // create text edit
				applyTextEdit(edits, unit, monitor) // apply text edit to compilation unit.
			} catch(MalformedTreeException exception) {
				logger.fatal(exception)
			} catch(CoreException exception) {
				logger.fatal(exception)
			}

		}
	}

	/** 
	 * Applies an {@link TextEdit} instance to an {@link ICompilationUnit}.
	 * @param edits is the {@link TextEdit} instance.
	 * @param unit is the {@link ICompilationUnit}.
	 * @throws JavaModelException if there is a problem with the JDT API.
	 */
	def static void applyTextEdit(TextEdit edits, ICompilationUnit unit, IProgressMonitor monitor) throws JavaModelException {
		var IDocument document = new Document(unit.source)
		try {
			edits.apply(document)
		} catch(MalformedTreeException exception) {
			logger.fatal(exception)
		} catch(BadLocationException exception) {
			logger.fatal(exception)
		}
		unit.buffer.setContents(document.get)
		unit.commitWorkingCopy(true, monitor)
	}

	/** 
	 * Visits all types of any {@link ICompilationUnit} of a {@link IPackageFragment} with a specific {@link ASTVisitor}and applies all recorded modifications to the Java files.
	 * @param unit is the {@link ICompilationUnit}.
	 * @param visitor is the specific {@link ASTVisitor}.
	 * @throws JavaModelException if there is a problem with the JDT API.
	 */
	def static void applyVisitorModifications(ICompilationUnit unit, ASTVisitor visitor, IProgressMonitor monitor) throws JavaModelException {
		var CompilationUnit parsedUnit = parse(unit, monitor)
		parsedUnit.recordModifications
		parsedUnit.accept(visitor)
		var TextEdit edits = parsedUnit.rewrite(new Document(unit.source), null)
		applyTextEdit(edits, unit, monitor)
		unit.commitWorkingCopy(true, monitor);
		unit.discardWorkingCopy();
	}

	/** 
	 * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
	 * @param unit is the {@link ICompilationUnit}.
	 * @param monitor is the {@link IProgressMonitor}.
	 * @return the {@link ASTNode}.
	 * @throws JavaModelException if there is problem with the Java model.
	 */
	def static CompilationUnit parse(ICompilationUnit unit, IProgressMonitor monitor) throws JavaModelException {
		unit.becomeWorkingCopy(monitor)
		var ASTParser parser = ASTParser.newParser(AST.JLS8)
		var Map<String, String> options = JavaCore.options
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options)
		parser.setCompilerOptions(options)
		parser.setKind(ASTParser.K_COMPILATION_UNIT)
		parser.setSource(unit)
		parser.setResolveBindings(true)
		return (parser.createAST(monitor) as CompilationUnit) // parse
	}

	/** 
	 * Logs the changed import if full logging is enabled in the {@link EcorificationProperties}.
	 */
	def private static void logChange(ICompilationUnit unit, ImportRewrite rewrite, IProgressMonitor monitor) {
		monitor.beginTask('''«unit.elementName»: remove «Arrays.toString(rewrite.removedImports)», add «Arrays.toString(rewrite.addedImports)»''', 0)
	}
}
