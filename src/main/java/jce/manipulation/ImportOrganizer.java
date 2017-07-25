package jce.manipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.text.edits.TextEdit;

import eme.generator.GeneratedEcoreMetamodel;
import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;
import jce.util.ProgressMonitorAdapter;

/**
 * Organizes the imports of the origin code.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction")  // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public class ImportOrganizer extends OriginCodeManipulator {

    private IProgressMonitor monitor;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public ImportOrganizer(EcorificationProperties properties) {
        super(properties);
        if (properties.get(BinaryProperty.FULL_LOGGING)) {
            monitor = new ProgressMonitorAdapter(logger);
        } else {
            monitor = new NullProgressMonitor();
        }
    }

    @Override
    protected void manipulate(ICompilationUnit unit, GeneratedEcoreMetamodel metamodel) throws JavaModelException {
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
}
