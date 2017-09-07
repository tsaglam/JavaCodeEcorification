package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.DEFAULT_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * {@link ASTVisitor} that makes all inner classes that have default visibility visible through changing the visibility
 * to public.
 * @author Timur Saglam
 */
public class ClassExpositionVisitor extends ASTVisitor {

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeDeclaration node) {
        if (isDefaultInnerClass(node)) { // if is default nested class
            AST ast = node.getAST(); // make public:
            node.modifiers().remove(ast.newModifier(DEFAULT_KEYWORD));
            node.modifiers().add(ast.newModifier(PUBLIC_KEYWORD));
        }
        return super.visit(node);
    }

    /**
     * Checks whether a {@link TypeDeclaration} is a default inner class, which means it is a member type, no interface,
     * and has the access level modifier default.
     */
    private boolean isDefaultInnerClass(TypeDeclaration node) {
        return node.isMemberTypeDeclaration() && !node.isInterface() && Modifier.isDefault(node.getModifiers());
    }
}