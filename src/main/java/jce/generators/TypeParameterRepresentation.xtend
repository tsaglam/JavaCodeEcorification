package jce.generators

import java.util.HashSet
import java.util.Set
import java.util.StringJoiner
import org.eclipse.emf.ecore.EGenericType
import org.eclipse.emf.ecore.ETypeParameter
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IImportDeclaration
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * This class models a type parameter of a wrapper class.
 * @author Timur Saglam
 */
@Accessors(PUBLIC_GETTER)
class TypeParameterRepresentation {
	Set<String> imports
	String content
	String name
	ETypeParameter eTypeParameter

	/**
	 * Creates new type parameter from an ETypeParameter and the compilation unit that contains the ecore interface.
	 */
	new(ETypeParameter eTypeParameter, ICompilationUnit ecoreInterface) {
		this.eTypeParameter = eTypeParameter
		name = eTypeParameter.name
		content = buildContent
		buildImports(ecoreInterface)
	}

	/**
	 * Builds type parameter with all bounds and their type parameters.
	 */
	def private String buildContent() {
		var result = eTypeParameter.name
		if(!eTypeParameter.EBounds.isNullOrEmpty) {
			val StringJoiner boundsJoiner = new StringJoiner(" & ")
			/*
			 * Slightly botched way to get the Java representation through the toString() method of EGenericType.
			 * It returns the Java representation at the end of the string in brackets with the comment "expression:".
			 * Therefore we drop everything except the bracket content without the "expression:" comment.
			 * TODO (MEDIUM) implement this in a less botched way.
			 */
			for (bound : eTypeParameter.EBounds) {
				val token = "(expression: "
				val boundString = bound.toString
				boundsJoiner.add(boundString.substring(boundString.indexOf(token) + token.length, boundString.length - 1))
			}
			result += " extends " + boundsJoiner.toString.replaceAll("\\bEString\\b", "String") // this is a side effect of the botched solution
		}
		return result
	}

	/** 
	 * Builds the list of types that need to be imported to use the type parameter.
	 */
	def private void buildImports(ICompilationUnit ecoreInterface) {
		imports = new HashSet
		for (import : ecoreInterface.imports) { // for every import
			for (bound : eTypeParameter.EBounds) { // for every type parameter bound
				checkGenericType(bound, import, ecoreInterface) // check if import is referenced
			}
		}
	}

	/**
	 * Recursion method for {@link TypeParameterRepresentation#buildImports()}. Checks whether a generic type or any its ETypeArguments is referenced by an import declaration.
	 */
	def private void checkGenericType(EGenericType type, IImportDeclaration importDeclaration, ICompilationUnit ecoreInterface) {
		checkImport(importDeclaration, type) // check if import is referenced
		for (argument : type.ETypeArguments) { // call this method recursivley on all type arguments:
			checkGenericType(argument, importDeclaration, ecoreInterface)
		}
	}
	
	/**
	 * Checks if a import declarations ends with the name of the EClassifier of an EGenericType (which is either a type parameter bound or a generic argument of a type parameter bound). If that is the case, the import will be added to the list of necessary imports.
	 */
	def private void checkImport(IImportDeclaration importDeclaration, EGenericType type) {
		val name = type.EClassifier?.name
		if(name !== null && importDeclaration.elementName.endsWith(name)) { // TODO (MEDIUM) when does this fail? (Generic Self Reference Test)
			imports.add(importDeclaration.elementName) // add to import string list.
		}
	}
}
