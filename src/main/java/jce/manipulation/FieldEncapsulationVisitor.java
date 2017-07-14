package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import jce.util.ProgressMonitorAdapter;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction")
public class FieldEncapsulationVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(FieldEncapsulationVisitor.class.getName());

    /**
     * Basic constructor.
     */
    public FieldEncapsulationVisitor() {
    }

    public void encapsulateField(IField field) {
        try {
            SelfEncapsulateFieldRefactoring refactoring = new SelfEncapsulateFieldRefactoring(field);
            CheckConditionsOperation checkCondOp = new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
            CreateChangeOperation createChangeOp = new CreateChangeOperation(checkCondOp, RefactoringStatus.WARNING);
            PerformChangeOperation performChangeOp = new PerformChangeOperation(createChangeOp);
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.run(performChangeOp, new ProgressMonitorAdapter(logger));
        } catch (JavaModelException exception) {
            exception.printStackTrace();
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            System.out.println("Node: " + node.getName()); // TODO
            for (FieldDeclaration field : node.getFields()) {
                System.out.println("Field: " + field); // TODO
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(0);
                IJavaElement element = fragment.resolveBinding().getJavaElement();
                if (element instanceof IField) {
                    encapsulateField((IField) element);
                } else {
                    throw new RuntimeException("Critical problem with field encapsulation. IJavaElement is not IField: " + element + " IS "
                            + element.getClass().getName()); // TODO
                }
            }
        }
        return super.visit(node);
    }
}