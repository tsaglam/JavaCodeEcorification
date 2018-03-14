package jce.generators

import java.util.LinkedList
import java.util.List
import java.util.StringJoiner
import org.eclipse.emf.ecore.ETypeParameter
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * This class models a type parameter of a wrapper class.
 * @author Timur Saglam
 */
@Accessors(PUBLIC_GETTER)
class TypeParameterRepresentation {
	List<String> imports
	String content
	String name
	ETypeParameter eTypeParameter

	/**
	 * Creates new type parameter from an ETypeParameter and the compilation unit that contains the ecore interface.
	 */
	new(ETypeParameter eTypeParameter, ICompilationUnit ecoreInterface) {
		this.eTypeParameter = eTypeParameter
		name = eTypeParameter.name
		content = buildContent()
		buildImports(ecoreInterface)
	}

	/** 
	 * Builds the list of types that need to be imported to use the type parameter.
	 */
	def private void buildImports(ICompilationUnit ecoreInterface) {
		imports = new LinkedList
		for (import : ecoreInterface.imports) { // for every import
			for (bound : eTypeParameter.EBounds) {
				if(import.elementName.endsWith(bound.EClassifier.name)) {
					imports.add(import.elementName) // add to import string list.
				}
			}
		}
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

}
