package jce.codemanipulation.ecore

import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.ReturnStatement

/**
 * {@link ASTVisitor} class that manipulates the init method of original Ecore factories in the generated code.
 * @author Heiko Klare
 */
class FactoryInitMethodCorrectionVisitor extends ASTVisitor {
	private static final String FACTORY_INIT_METHOD_NAME = "init";
	
	/**
	 * Removes the namespace-URI-based factory determination logic from the original Ecore factories, 
	 * because they are are only used internally and are not registered globally. 
	 */
	public override boolean visit(MethodDeclaration node) {
		if (node.name.toString == FACTORY_INIT_METHOD_NAME) {
			node.body.statements.removeIf[!(it instanceof ReturnStatement)]
		}
		return true;
	}
}
