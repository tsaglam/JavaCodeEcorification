package jce.manipulation;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MonitorFactory;
import jce.util.PackageFilter;
import jce.util.ResourceRefresher;

/**
 * Base class for manipulating origin code.
 * @author Timur Saglam
 */
public abstract class OriginCodeManipulator {
    protected static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());
    protected final EcorificationProperties properties;
    protected final IProgressMonitor monitor;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public OriginCodeManipulator(EcorificationProperties properties) {
        this.properties = properties;
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
    }

    /**
     * Manipulates the origin code of the given {@link IProject}.
     * @param project is the given {@link IProject}.
     */
    public void manipulate(IProject project) {
        logger.info("Starting the origin code manipulation " + getClass().getSimpleName());
        ResourceRefresher.refresh(project);
        List<IPackageFragment> packages = PackageFilter.startsNotWith(project, properties.get(TextProperty.ECORE_PACKAGE),
                properties.get(TextProperty.WRAPPER_PACKAGE));
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
     * Visits all types of any {@link ICompilationUnit} of a {@link IPackageFragment} with a specific {@link ASTVisitor}
     * and applies all recorded modifications to the Java files.
     * @param unit is the {@link ICompilationUnit}.
     * @param visitor is the specific {@link ASTVisitor}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    protected void applyVisitorModifications(ICompilationUnit unit, ASTVisitor visitor) throws JavaModelException {
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
        unit.commitWorkingCopy(true, monitor);
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
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(monitor); // parse
    }
}