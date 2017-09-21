package jce.codemanipulation;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.text.edits.TextEdit;

import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.ASTUtil;
import jce.util.jdt.PackageFilter;
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
            ASTUtil.applyTextEdit(edit, unit, monitor);
        } catch (OperationCanceledException exception) {
            logger.error(exception);
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }
}
