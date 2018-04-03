package jce.codemanipulation.ecore

import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.ICompilationUnit
import jce.properties.EcorificationProperties
import jce.properties.TextProperty
import org.eclipse.jdt.core.dom.SimpleType
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.CastExpression
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.Type
import org.eclipse.jdt.core.dom.QualifiedName
import org.eclipse.jdt.core.dom.ImportDeclaration
import jce.util.PathHelper

/**
 * This visitors replaces all references to the factory class within a package interface or
 * package implementation class to the newly generated factory. The old factory has a suffix,
 * which is replaced in imports, member references, return types and casts.
 * 
 * @author Heiko Klare
 */
class PackageImplFactoryCorrectionVisitor extends ASTVisitor {
	public static val PACKAGE_IMPL_SUFFIX = "PackageImpl";
	public static val PACKAGE_SUFFIX = "Package";
	private static val FACTORY_SUFFIX = "Factory";
	private val String factoryName;
	private val ICompilationUnit compilationUnit;
	private val EcorificationProperties properties;
	private val PathHelper pathHelper;
	
	new(ICompilationUnit compilationUnit, EcorificationProperties properties) {
		this.properties = properties;
		this.compilationUnit = compilationUnit;
		this.pathHelper = new PathHelper(".")
		val compilationUnitName = pathHelper.cutLastSegment(compilationUnit.elementName);
		this.factoryName = if (compilationUnit.elementName.contains(PACKAGE_IMPL_SUFFIX)) {
			compilationUnitName.substring(0, compilationUnitName.length - PACKAGE_IMPL_SUFFIX.length) + FACTORY_SUFFIX
		} else {
			compilationUnitName.substring(0, compilationUnitName.length - PACKAGE_SUFFIX.length) + FACTORY_SUFFIX
		}
	}
	
	/**
	 * Corrects the import declaration of the old factory to import the newly generated one instead. 
	 */
	override visit(ImportDeclaration node) {
		if (node.name.fullyQualifiedName.contains(factoryName)) {
			node.name = node.AST.newName(node.name.fullyQualifiedName.removeOldFactorySuffix);
		}
		return true;
	}
	
	/**
	 * Corrects references to static variables of the factory class, especially to the <code>eINSTANCE</code> field.
	 */
	override visit(QualifiedName node) {
		if (node.qualifier.fullyQualifiedName.contains(factoryName)) {
			node.qualifier = node.AST.newSimpleName(node.qualifier.fullyQualifiedName.removeOldFactorySuffix);
		}
		return true;
	}

	/**
	 * Corrects the return type of methods to use the newly generated factory type. This is especially necessary
	 * for the factory getter method.
	 */
	override visit(MethodDeclaration node) {
		val newType = node.returnType2.modifyType(node.AST);
		if (newType !== null) {
			node.returnType2 = newType;
		}
		 
		return true;
	}

	/**
	 * Corrects casts to the factory class to use the newly generated factory type. This is especially necessary
	 * within the factory getter method.
	 */
	override visit(CastExpression node) {
		val newType = node.type.modifyType(node.AST);
		if (newType !== null) {
			node.type = newType;
		}

		return true;
	}
	
	/**
	 * Returns a type referencing the newly generated factory given the {@link SimpleType} of the old factory.
	 * Otherwise <code>null</code> is returned.
	 */
	private def Type modifyType(Type type, AST ast) {
		if (type instanceof SimpleType) {
			if (type.name.fullyQualifiedName.contains(factoryName)) {
				return ast.newSimpleType(ast.newSimpleName(type.name.fullyQualifiedName.removeOldFactorySuffix));
			}
		}
		return null;
	}
	
	/**
	 * Removes the old factory suffic from the given factory name.
	 */
	private def String removeOldFactorySuffix(String oldFactoryName) {
		return oldFactoryName.replace(FACTORY_SUFFIX + properties.get(TextProperty.FACTORY_SUFFIX), FACTORY_SUFFIX);
	}
}