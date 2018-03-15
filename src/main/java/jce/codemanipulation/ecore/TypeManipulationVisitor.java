package jce.codemanipulation.ecore;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import jce.util.PathHelper;
import jce.util.RawTypeUtil;

/**
 * TODO (HIGH) comment.
 * @author Timur Saglam
 */
public class TypeManipulationVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(TypeManipulationVisitor.class.getName());
    private final String typeName;
    private final PathHelper pathHelper;

    /**
     * Basic constructor. TODO (HIGH) comment.
     * @param typeName
     */
    public TypeManipulationVisitor(String typeName) {
        super();
        this.typeName = typeName;
        this.pathHelper = new PathHelper('.');
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        for (SingleVariableDeclaration parameter : RawTypeUtil.castList(SingleVariableDeclaration.class, node.parameters())) {
            if (pathHelper.getLastSegment(typeName).equals(parameter.getType().toString())) {
                AST ast = node.getAST();
                parameter.setType(ast.newSimpleType(ast.newName(typeName)));
                logger.info("Manually changed type of parameter " + parameter.getName() + " to " + typeName);
            }
        }
        return super.visit(node);
    }
}