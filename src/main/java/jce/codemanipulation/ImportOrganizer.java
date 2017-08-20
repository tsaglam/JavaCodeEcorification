package jce.codemanipulation;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.CorextMessages;
import org.eclipse.jdt.internal.corext.ValidateEditException;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.text.edits.TextEdit;

import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.PackageFilter;
import jce.util.logging.MonitorFactory;

/**
 * Organizes the imports of the origin code.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction")  // TODO (LOW) This class uses LTK classes & methods that are not marked as API
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

    /**
     * Applies {@link TextEdit} to {@link ICompilationUnit} and saves the changes. Taken from JavaModelUtil, because
     * {@link JavaModelUtil#applyEdit()} applyEdit is gone in Oxygen.
     */
    private void applyEdit(ICompilationUnit unit, TextEdit edit) throws CoreException, ValidateEditException {
        IFile file = (IFile) unit.getResource();
        if (!file.exists()) {
            unit.applyTextEdit(edit, monitor);
        } else {
            monitor.beginTask(CorextMessages.JavaModelUtil_applyedit_operation, 2);
            try {
                IStatus status = Resources.makeCommittable(file, null);
                if (!status.isOK()) {
                    throw new ValidateEditException(status);
                }
                unit.applyTextEdit(edit, monitor);
                unit.save(monitor, true);
            } finally {
                monitor.done();
            }
        }
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsNotWith(project, properties.get(TextProperty.WRAPPER_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        unit.becomeWorkingCopy(monitor); // changes unit handle to working copy
        CompilationUnit reconciledUnit = unit.reconcile(AST.JLS8, false, null, monitor); // Don't ask me.
        try {
            OrganizeImportsOperation operation = new OrganizeImportsOperation(unit, reconciledUnit, true, true, true, null);
            TextEdit edit = operation.createTextEdit(monitor);
            applyEdit(unit, edit);
            unit.commitWorkingCopy(true, monitor);
            unit.save(monitor, true);
        } catch (OperationCanceledException exception) {
            logger.error(exception);
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }
}
