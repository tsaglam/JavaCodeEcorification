package jce.codemanipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.text.edits.TextEdit;

import jce.properties.EcorificationProperties;
import jce.util.jdt.ASTUtil;
import jce.util.logging.MonitorFactory;

/**
 * Organizes the imports of the whole source code of a project.
 * @author Timur Saglam
 */
public class ImportOrganizer extends AbstractCodeManipulator {
    private static final Logger logger = LogManager.getLogger(AbstractCodeManipulator.class.getName());
    private IProgressMonitor monitor;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public ImportOrganizer(EcorificationProperties properties) {
        super(properties);
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        unit.becomeWorkingCopy(monitor); // changes unit handle to working copy
        CompilationUnit reconciledUnit = unit.reconcile(AST.JLS10, false, null, monitor);
        try {
            OrganizeImportsOperation operation = new OrganizeImportsOperation(unit, reconciledUnit, true, true, true, null);
            TextEdit edit = operation.createTextEdit(monitor);
            ASTUtil.applyTextEdit(edit, unit, monitor);
        } catch (OperationCanceledException exception) {
            logger.error(exception);
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }
}
