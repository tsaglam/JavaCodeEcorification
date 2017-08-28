package jce.codemanipulation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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

import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;
import jce.util.PathHelper;
import jce.util.ResourceRefresher;
import jce.util.logging.MonitorFactory;

/**
 * Base class for code manipulation. Can be extended for specific code manipulator classes. Offers functionality for
 * applying text edits and visitor modifications to any {@link ICompilationUnit}.
 * @author Timur Saglam
 */
public abstract class AbstractCodeManipulator {
    protected Logger logger;
    protected final IProgressMonitor monitor;
    protected final EcorificationProperties properties;
    protected PathHelper nameUtil;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public AbstractCodeManipulator(EcorificationProperties properties) {
        this.properties = properties;
        logger = LogManager.getLogger(this.getClass().getName());
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
        nameUtil = new PathHelper('.');
    }

    /**
     * Manipulates the code of the given {@link IProject}.
     * @param project is the given {@link IProject}.
     */
    public void manipulate(IProject project) {
        logger.info("Starting the code manipulation " + getClass().getSimpleName() + "...");
        ResourceRefresher.refresh(project);
        List<IPackageFragment> packages = filterPackages(project, properties);
        try {
            for (IPackageFragment fragment : packages) {
                if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    for (ICompilationUnit unit : fragment.getCompilationUnits()) {
                        manipulate(unit);
                    }
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Logs the changed import if full logging is enabled in the {@link EcorificationProperties}.
     */
    private void logChange(ICompilationUnit unit, ImportRewrite rewrite) {
        if (properties.get(BinaryProperty.FULL_LOGGING)) {
            logger.info(unit.getElementName() + ": removed " + Arrays.toString(rewrite.getRemovedImports()) + ", added "
                    + Arrays.toString(rewrite.getAddedImports()));
        }
    }

    /**
     * Applies all recorded changes of an {@link ImportRewrite} to an {@link ICompilationUnit}.
     * @param unit is the {@link ICompilationUnit}.
     * @param importRewrite is the {@link ImportRewrite}.
     */
    protected void applyImportRewrite(ICompilationUnit unit, ImportRewrite importRewrite) {
        if (importRewrite.hasRecordedChanges()) { // apply changes if existing
            logChange(unit, importRewrite); // log the changed imports
            try {
                TextEdit edits = importRewrite.rewriteImports(monitor); // create text edit
                applyTextEdit(edits, unit); // apply text edit to compilation unit.
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
    protected void applyTextEdit(TextEdit edits, ICompilationUnit unit) throws JavaModelException {
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
    protected void applyVisitorModifications(ICompilationUnit unit, ASTVisitor visitor) throws JavaModelException {
        CompilationUnit parsedUnit = parse(unit);
        parsedUnit.recordModifications();
        parsedUnit.accept(visitor);
        TextEdit edits = parsedUnit.rewrite(new Document(unit.getSource()), null);
        applyTextEdit(edits, unit);
    }

    /**
     * Defines the {@link IPackageFragment} list which will be manipulated.
     * @param project is the {@link IProject} to manipulate.
     * @param properties are the {@link EcorificationProperties}.
     * @return the target packages of this code manipulator.
     */
    protected abstract List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties);

    /**
     * Returns the name of the package member type of a compilation unit. E.g. "model.Main" from "Main.java"
     * @param unit is the {@link ICompilationUnit}.
     * @return the name as a String.
     * @throws JavaModelException if there are problems with the Java model.
     */
    protected String getPackageMemberName(ICompilationUnit unit) throws JavaModelException {
        String packageName = unit.getParent().getElementName();
        String memberName = nameUtil.cutLastSegment(unit.getElementName()); // cut the filename extension
        return nameUtil.append(packageName, memberName);
    }

    /**
     * Executes the origin code manipulation on a compilation unit.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    protected abstract void manipulate(ICompilationUnit unit) throws JavaModelException;

    /**
     * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
     * @param unit is the {@link ICompilationUnit}.
     * @return the {@link ASTNode}.
     * @throws JavaModelException if there is problem with the Java model.
     */
    protected CompilationUnit parse(ICompilationUnit unit) throws JavaModelException {
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
