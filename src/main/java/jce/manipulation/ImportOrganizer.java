package jce.manipulation;

import java.util.List;

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
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
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
public class ImportOrganizer extends CodeManipulator {

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
        unit.becomeWorkingCopy(monitor); // changes compilation unit handle to working copy
        CompilationUnit reconciledUnit = unit.reconcile(AST.JLS8, false, null, monitor); // don't ask me
        try {
            OrganizeImportsOperation operation = new OrganizeImportsOperation(unit, reconciledUnit, true, true, true, null);
            TextEdit edit = operation.createTextEdit(monitor);
            JavaModelUtil.applyEdit(unit, edit, true, monitor);
            unit.commitWorkingCopy(true, monitor);
            unit.save(monitor, true);
        } catch (OperationCanceledException exception) {
            exception.printStackTrace();
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsNotWith(project, properties.get(TextProperty.WRAPPER_PACKAGE));
    }
}
