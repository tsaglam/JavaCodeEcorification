package jce.generators

import java.util.LinkedList
import java.util.List
import jce.properties.EcorificationProperties
import jce.util.ASTUtil
import jce.util.logging.MonitorFactory
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.xtend.lib.annotations.Accessors
import java.lang.reflect.Modifier
import org.eclipse.jdt.core.ICompilationUnit

final class ConstructorGenerator {
	static final Logger logger = LogManager.getLogger(ConstructorGenerator.name)

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	/**
	 * Builds the constructor representations from all constructors of an IType. The IType is the correlating type to
	 * the wrapper which should use the generated constructors.
	 */
	def static List<WrapperConstructor> generate(String typeName, IJavaProject project,
		EcorificationProperties properties) {
		val IType type = project.findType(typeName)
		val ConstructorVisitor visitor = new ConstructorVisitor(type)
		val IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties)
		val CompilationUnit parsedUnit = ASTUtil.parse(type.compilationUnit, monitor)
		parsedUnit.accept(visitor)
		return visitor.constructors
	}

	/**
	 * AST visitor that creates WrapperConstructors for every constructor of an compilation unit.
	 */
	@Accessors(PUBLIC_GETTER)
	static class ConstructorVisitor extends ASTVisitor {
		List<WrapperConstructor> constructors
		ICompilationUnit unit

		/**
		 * Basic constructor, creates WrapperConstructor list.
		 */
		new(IType type) {
			unit = type.compilationUnit
			constructors = new LinkedList
		}

		override visit(MethodDeclaration node) {
			if(node.isConstructor && Modifier.isPublic(node.getModifiers)) {
				constructors.add(new WrapperConstructor(node, unit))
			}
			return false
		}
	}
}
