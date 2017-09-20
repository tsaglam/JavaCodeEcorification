package jce.generators

import java.util.LinkedList
import java.util.List
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * AST visitor that creates WrapperConstructors for every constructor of an compilation unit.
 */
 @Accessors(PUBLIC_GETTER)
class ConstructorVisitor extends ASTVisitor {
	List<WrapperConstructor> constructors

	/**
	 * Basic constructor, creates WrapperConstructor list.
	 */
	new() {
		constructors = new LinkedList
	}

	override visit(MethodDeclaration node) {
		if(node.isConstructor) {
			constructors.add(new WrapperConstructor(node))
		}
		return false
	}
}
