package jce.codemanipulation.ecore

import jce.properties.EcorificationProperties
import jce.util.PathHelper
import jce.util.RawTypeUtil
import jce.util.jdt.TypeUtil
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.emf.ecore.EObject
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IImportDeclaration
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.ArrayType
import org.eclipse.jdt.core.dom.Name
import org.eclipse.jdt.core.dom.ParameterizedType
import org.eclipse.jdt.core.dom.SimpleType
import org.eclipse.jdt.core.dom.Type
import org.eclipse.jdt.core.dom.TypeDeclaration
import org.eclipse.jdt.core.dom.TypeParameter

/** 
 * {@link ASTVisitor} class that retains the super interface declarations and type parameter bounds of the Ecore code types.
 * The interface declarations and type parameter bounds are made independent from the imports of the class.
 * Therefore the import manipulation does not affect them.
 * @author Timur Saglam
 */
class TypeRetentionVisitor extends ASTVisitor {
	static final Logger logger = LogManager.getLogger(TypeRetentionVisitor.name)
	final String currentPackage
	final IImportDeclaration[] imports
	final PathHelper pathHelper

	/** 
	 * Basic constructor. Creates the visitor.
	 * @param compilationUnit is the {@link ICompilationUnit} from which the import declarations and the current package
	 * are resolved.
	 * @param properties are the {@link EcorificationProperties}.
	 * @throws JavaModelException if there is a problem with the {@link ICompilationUnit}.
	 */
	new(ICompilationUnit compilationUnit, IImportDeclaration[] imports) throws JavaModelException {
		super()
		this.imports = imports
		currentPackage = compilationUnit.parent.elementName
		pathHelper = new PathHelper('.')
	}

	/**
	 * Visits all type declarations and retains the super interface types.
	 */
	override boolean visit(TypeDeclaration node) {
		for (Type superInterface : RawTypeUtil.castList(Type, node.superInterfaceTypes)) {
			retainSuperInterface(superInterface, node, node.AST)
		}
		return super.visit(node)
	}

	/**
	 * Visits all type parameter declarations and retains bound types.
	 */
	override boolean visit(TypeParameter node) {
		for (Type bound : RawTypeUtil.castList(Type, node.typeBounds)) {
			retainBoundType(bound)
		}
		return super.visit(node)
	}

	/**
	 * Deals with every other subclass of {@link Type}.
	 */
	def private dispatch void retainBoundType(Type bound) {
		logger.error("Error: Could not retain type parameter bound " + bound)
	}

	/**
	 * Deals with any {@link ParameterizedType}. Retains its type and the type of its parameters.
	 */
	def private dispatch void retainBoundType(ParameterizedType bound) {
		retainBoundType(bound.type) // retain the type itself
		for (Type argument : RawTypeUtil.castList(Type, bound.typeArguments)) {
			retainBoundType(argument) // retain all the arguments
		}
	}

	/**
	 * Deals with any {@link ArrayType}. Retains its element type.
	 */
	def private dispatch void retainBoundType(ArrayType bound) {
		retainBoundType(bound.elementType) // retain only the element type of the array
	}

	/**
	 * Deals with any {@link SimpleType}. Retains the type by replacing the type name through its fully qualified name.
	 */
	def private dispatch void retainBoundType(SimpleType bound) {
		var Name newName = bound.AST.newName(resolveName(bound)) // create new name
		bound.setName(newName) // set the new name of the bound type
	}

	/** 
	 * Changes the a super interface declaration of a {@link TypeDeclaration} to contain the fully qualified name.
	 */
	@SuppressWarnings("unchecked") def private void retainSuperInterface(Type oldInterface, TypeDeclaration node, AST ast) {
		var SimpleType interfaceType = TypeUtil.getSimpleType(oldInterface)
		if(isNotEObject(interfaceType)) {
			var String newName = resolveInterfaceName(interfaceType)
			var Type newSuperInterface = ast.newSimpleType(ast.newName(newName))
			newSuperInterface = copyParameters(oldInterface, newSuperInterface, ast)
			node.superInterfaceTypes.remove(oldInterface)
			node.superInterfaceTypes.add(newSuperInterface)
		}
	}

	/** 
	 * Adds all type parameters the old super interface to the new super interface.
	 */
	@SuppressWarnings("unchecked") def private Type copyParameters(Type oldInterface, Type newInterface, AST ast) {
		if(oldInterface.isParameterizedType) { // if interface has parameters
			var ParameterizedType parameterizedType = ast.newParameterizedType(newInterface) // parameterized new interface
			var ParameterizedType castedType = (oldInterface as ParameterizedType) // cast old interface
			for (Type type : RawTypeUtil.castList(Type, castedType.typeArguments)) {
				type.delete // delete type parameter from old interface
				parameterizedType.typeArguments.add(type) // and add to the new one
			}
			return parameterizedType // use parameterized type with copied parameters
		}
		return newInterface // just use the original type
	}

	/** 
	 * Returns fully qualified name of a type in a super interface declaration.
	 * The name is resolved from the import declarations and the current package.
	 */
	def private String resolveInterfaceName(SimpleType type) {
		if (type.name.qualifiedName) {
			return type.name.fullyQualifiedName
		}
		return nameFromImports(type) ?: pathHelper.append(currentPackage, type.name.fullyQualifiedName)
	}

	/** 
	 * Returns fully qualified name of a {@link SimpleType}. The name is resolved from the import declarations.
	 * If the import declarations do not contain this name, the original name is returned.
	 */
	def private String resolveName(SimpleType type) {
		return nameFromImports(type) ?: type.name.fullyQualifiedName
	}

	/**
	 * Returns the fully qualified name of a {@link SimpleType} from the imports, or null if the imports do not contain such type.
	 */
	def private String nameFromImports(SimpleType type) {
		// fully qualified name may also be simple, then we need to also match the dot to avoid equal suffix matches
		val endsWithMatch = (if (type.name.simpleName) "." else "") + type.name.fullyQualifiedName
		for (IImportDeclaration declaration : imports) {
			if (declaration.elementName.endsWith(endsWithMatch)) {
				return declaration.elementName
			}
		}
		return null
	}

	/** 
	 * Checks whether a type name matches the name of {@link EObject}.
	 */
	def private boolean isNotEObject(SimpleType type) {
		return !EObject.simpleName.equals(type.name.fullyQualifiedName)
	}
}
