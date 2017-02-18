package jce.manipulation;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class TypeVisitor extends ASTVisitor {
    private final String currentPackage;

    /**
     * Basic constructor.
     * @param currentPackage is the current package.
     */
    public TypeVisitor(String currentPackage) {
        this.currentPackage = currentPackage;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            setSuperClass(node, "wrappers." + currentPackage + "." + node.getName().toString() + "Wrapper");
        }
        return super.visit(node);
    }

    /**
     * Sets a qualified name as super class of a type declaration.
     * @param declaration is the type declaration.
     * @param qualifiedName is the qualified name.
     */
    public void setSuperClass(TypeDeclaration declaration, String qualifiedName) {
        AST ast = declaration.getAST();
        Name name = ast.newName(qualifiedName);
        Type type = ast.newSimpleType(name);
        declaration.setSuperclassType(type);
    }
}