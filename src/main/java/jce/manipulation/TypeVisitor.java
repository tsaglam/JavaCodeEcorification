package jce.manipulation;

import java.awt.Window.Type;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class TypeVisitor extends ASTVisitor {
    private final String currentPackage;
    private final IJavaProject project;

    /**
     * Basic constructor.
     * @param currentPackage is the current package.
     * @param project is the current {@link IProject}.
     */
    public TypeVisitor(String currentPackage, IJavaProject project) {
        this.currentPackage = currentPackage;
        this.project = project;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // is class
            // TODO (HIGH) change inheritance if it has a ecore equivalent.
            System.err.print(node.getName().getFullyQualifiedName());
            System.err.print(" is a ");
            System.err.print(node.getSuperclassType());
            System.err.print(" in ");
            System.err.println(currentPackage);
            try {
                System.err.println("SEARCH: " + "wrappers." + currentPackage + "." + node.getName().toString() + "Wrapper");
                IType result = project.findType("wrappers." + currentPackage + "." + node.getName().toString() + "Wrapper");
                if (result != null) {
                    System.err.println("   FOUND" + result.getFullyQualifiedName());
                } else {
                    System.err.println("   NO RESULT");
                }
            } catch (JavaModelException exception) {
                exception.printStackTrace();
            }
        }
        return super.visit(node);
    }
}