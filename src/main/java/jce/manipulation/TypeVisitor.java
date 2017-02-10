package jce.manipulation;

import java.awt.Window.Type;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class TypeVisitor extends ASTVisitor {
    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // is class
            // TODO (HIGH) change inheritance if it has a ecore equivalent.
            System.err.print(node.getName());
            System.err.print(" is a ");
            System.err.println(node.getSuperclassType());
        }
        return super.visit(node);
    }
}