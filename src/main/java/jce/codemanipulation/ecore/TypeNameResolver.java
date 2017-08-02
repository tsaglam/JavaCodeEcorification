package jce.codemanipulation.ecore;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * AST visitor that resolves the name of the package member type of the compilation unit.
 * @author Timur Saglam
 */
public class TypeNameResolver extends ASTVisitor {
    private String typeName;

    /**
     * Getter for the resolved type name.
     * @return the fully qualified name of the resolved type binding of the type declaration.
     */
    public String getTypeName() {
        if (typeName == null) {
            throw new IllegalStateException("Visit a compilation unit before retrieving the name.");
        }
        return typeName;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.isPackageMemberTypeDeclaration()) {
            typeName = node.getName().resolveTypeBinding().getQualifiedName(); // fully qualified name of class
        }
        return super.visit(node);
    }
}