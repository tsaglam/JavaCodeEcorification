package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.jdt.ASTUtil;

/**
 * This class generates default constructors for every class that does not support default constructors. This is a
 * unstable fix for the problem that Ecore factories require default constructors to create instances.
 * @author Timur Saglam
 */
public class DefaultConstructorGenerator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public DefaultConstructorGenerator(EcorificationProperties properties) {
        super(properties);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        ASTUtil.applyVisitorModifications(unit, new ConstructorGenerationVisitor(), monitor);
    }

    /**
     * {@link ASTVisitor} that generates default constructors if they are missing.
     * @author Timur Saglam
     */
    private class ConstructorGenerationVisitor extends ASTVisitor {

        @Override
        public boolean visit(TypeDeclaration node) { // TODO (HIGH) what if def const is private?
            if (node.isPackageMemberTypeDeclaration() && !hasDefaultConstructor(node)) {
                addDefaultConstructor(node);
            }
            return false;
        }

        /**
         * Adds a new public default constructor with an empty body to a {@link TypeDeclaration}.
         */
        @SuppressWarnings("unchecked")
        private void addDefaultConstructor(TypeDeclaration node) {
            AST ast = node.getAST();
            MethodDeclaration constructor = ast.newMethodDeclaration(); // create constructor
            constructor.setConstructor(true); // differentiate from normal methods
            constructor.setName(ast.newSimpleName(node.getName().getIdentifier())); // set name
            constructor.setBody(ast.newBlock()); // add empty method body
            constructor.modifiers().add(ast.newModifier(PUBLIC_KEYWORD)); // make public
            node.bodyDeclarations().add(constructor); // add to node
        }

        /**
         * Checks whether a type has a default constructor by checking whether one of its methods both is a constructor
         * and has no parameters.
         */
        private boolean hasDefaultConstructor(TypeDeclaration type) {
            for (MethodDeclaration method : type.getMethods()) {
                if (method.isConstructor() && method.parameters().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
