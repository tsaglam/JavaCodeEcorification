package jce.manipulation;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class MemberRemovalVisitor extends ASTVisitor {

    private List<String> removedFields;

    // Basic constructor.
    public MemberRemovalVisitor() {
        removedFields = new LinkedList<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            removeFields(node);
            removeAccessMethods(node);
        }
        return super.visit(node);
    }

    /**
     * Capitalizes the first letter of a String.
     */
    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Checks whether a {@link MethodDeclaration} is an access method.
     */
    private boolean isAccessMethod(MethodDeclaration method) {
        String[] prefixes = { "get", "set", "is" };
        for (String prefix : prefixes) {
            for (String field : removedFields) {
                if (method.getName().getIdentifier().equals(prefix + capitalize(field))) {
                    return true;
                }
            }
        }
        return false; // TODO !!! CONTINUE HERE !!!
    }

    /**
     * Removes any {@link MethodDeclaration} of a {@link TypeDeclaration} which is an access method for a previously
     * removed {@link FieldDeclaration}.
     */
    private void removeAccessMethods(TypeDeclaration type) {
        for (MethodDeclaration method : type.getMethods()) {
            if (isAccessMethod(method)) {
                method.delete();
            }
        }
    }

    /**
     * Removes any non-static {@link FieldDeclaration} of a {@link TypeDeclaration}. Saves the identifiers of the
     * removed fields in the {@link MemberRemovalVisitor#removedFields} list.
     */
    private void removeFields(TypeDeclaration type) {
        for (FieldDeclaration field : type.getFields()) { // for every field
            if (!Modifier.isStatic(field.getModifiers())) { // if not static
                field.delete(); // delete
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(0);
                System.err.println(fragment.getName()); // TODO
                System.err.println(fragment.getName().getIdentifier()); // TODO
                System.err.println(fragment.getName().getFullyQualifiedName()); // TODO
                removedFields.add(fragment.getName().getIdentifier());
            }
        }
    }
}