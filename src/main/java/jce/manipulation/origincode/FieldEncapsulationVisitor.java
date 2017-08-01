package jce.manipulation.origincode;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import jce.properties.EcorificationProperties;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} class for the encapsulation of fields. Encapsulates all field of an class upon visiting.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction") // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public class FieldEncapsulationVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(FieldEncapsulationVisitor.class.getName());
    private IProgressMonitor monitor;

    public FieldEncapsulationVisitor(EcorificationProperties properties) {
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
    }

    /**
     * Encapsulates a specific {@link IField}.
     * @param field is the specific {@link IField}.
     */
    public void encapsulateField(IField field) {
        try {
            SelfEncapsulateFieldRefactoring refactoring = new SelfEncapsulateFieldRefactoring(field);
            CheckConditionsOperation checkCondOp = new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
            CreateChangeOperation createChangeOp = new CreateChangeOperation(checkCondOp, RefactoringStatus.WARNING);
            PerformChangeOperation performChangeOp = new PerformChangeOperation(createChangeOp);
            ResourcesPlugin.getWorkspace().run(performChangeOp, monitor);
        } catch (JavaModelException exception) {
            exception.printStackTrace();
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            for (FieldDeclaration field : node.getFields()) { // for every field:
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(0);
                IJavaElement element = fragment.resolveBinding().getJavaElement(); // parse FieldDeclaration to IField
                if (element instanceof IField) {
                    encapsulateField((IField) element); // Encapsulate if casted succesful.
                } else {
                    throw new ClassCastException("IJavaElement is not IField: " + element + " is " + element.getClass().getName());
                }
            }
        }
        return super.visit(node);
    }
}