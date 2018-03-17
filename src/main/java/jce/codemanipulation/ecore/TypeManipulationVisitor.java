package jce.codemanipulation.ecore;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import jce.util.PathHelper;
import jce.util.RawTypeUtil;

/**
 * Visitor that replaces the Ecore types of method parameters and return types through their correlating origin code
 * types. The types that are replaced are all types in an Ecore interface that have the type of the interface itself.
 * these type cannot manipulated through changing the imports because this would lead to an import conflict. therefore
 * the parameters and return type declarations are manipulated directly.
 * @author Timur Saglam
 */
public class TypeManipulationVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(TypeManipulationVisitor.class.getName());
    private final String originType;
    private final PathHelper pathHelper;

    /**
     * Basic constructor. Sets the name of the origin type.
     * @param originType is the fully qualified name of the correlating origin type of the Ecore interface which is
     * visited.
     */
    public TypeManipulationVisitor(String originType) {
        super();
        this.originType = originType;
        pathHelper = new PathHelper('.');
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        AST ast = node.getAST();
        checkParameters(node, ast);
        checkReturnType(node, ast);
        return super.visit(node);
    }

    /**
     * Checks if parameters are self references and replaces them if they are.
     */
    private void checkParameters(MethodDeclaration method, AST ast) {
        for (SingleVariableDeclaration parameter : RawTypeUtil.castList(SingleVariableDeclaration.class, method.parameters())) {
            if (isSelfReference(parameter.getType())) {
                parameter.setType(createOriginType(ast));
                logger.info("Manually changed type of parameter " + parameter.getName() + " to " + originType);
            }
        }
    }

    /**
     * Checks if the method return type is a self references and replaces it if it is.
     */
    private void checkReturnType(MethodDeclaration method, AST ast) {
        if (isSelfReference(method.getReturnType2())) {
            method.setReturnType2(createOriginType(ast));
            logger.info("Manually changed return type of method " + method.getName() + " to " + originType);
        }
    }

    /**
     * Creates a new {@link Type} which referenced the correlating origin code type of the visited Ecore interface.
     */
    private Type createOriginType(AST ast) {
        return ast.newSimpleType(ast.newName(originType));
    }

    /**
     * Checks if the type has the same name as the containing Ecore interface.
     */
    private boolean isSelfReference(Type type) {
        return pathHelper.getLastSegment(originType).equals(type.toString());
    }
}