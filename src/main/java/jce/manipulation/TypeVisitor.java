package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.text.edits.TextEdit;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class TypeVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(TypeVisitor.class.getName());
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
        if (!node.isInterface()) { // is class
            System.err.print(node.getName().getFullyQualifiedName()); // TODO (HIGH) remove debug output
            System.err.print(" is a ");
            System.err.print(node.getSuperclassType());
            System.err.print(" in ");
            System.err.println(currentPackage);
            setSuperClass(node, "wrappers." + currentPackage + "." + node.getName().toString() + "Wrapper");
        }
        return super.visit(node);
    }

    void setSuperClass(TypeDeclaration typeDecl, String qualifiedName) {
        System.err.println("   try to set: " + qualifiedName);
        AST ast = typeDecl.getAST();
        Name name = ast.newName(qualifiedName);
        Type type = ast.newSimpleType(name);
        typeDecl.setSuperclassType(type);
        System.err.println("   is: " + typeDecl.getSuperclassType());
        ASTRewrite astRewriter = ASTRewrite.create(ast);
        try {
            TextEdit edits = astRewriter.rewriteAST(); // TODO CONTINUE HERE
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        } catch (IllegalArgumentException exception) {
            logger.fatal(exception);
        }
    }
}