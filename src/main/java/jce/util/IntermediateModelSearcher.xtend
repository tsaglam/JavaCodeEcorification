package jce.util

import eme.model.IntermediateModel
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EStructuralFeature
import eme.model.ExtractedType
import eme.model.datatypes.ExtractedField
import org.eclipse.emf.ecore.ETypeParameter
import eme.model.datatypes.ExtractedTypeParameter
import org.eclipse.emf.ecore.EPackage

/**
 * Utility for finding the counterparts of Ecore elements in an intermediate model.
 */
final class IntermediateModelSearcher {

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/**
	 * Finds the correlating ExtractedType of an EClass in an intermediate model.
	 */
	def static ExtractedType findType(EClass eClass, IntermediateModel model) {
		val PathHelper helper = new PathHelper('.')
		var String fullyQualifiedName = ""
		var EPackage current = eClass.EPackage
		while (current !== null) { // iterate through package hierarchy
			fullyQualifiedName = helper.append(current.name, fullyQualifiedName) // concatenate package with super package name
			current = current.ESuperPackage
		}
		fullyQualifiedName = helper.append(helper.cutFirstSegment(fullyQualifiedName), eClass.name) // cut Ecore package name
		return model.getType(fullyQualifiedName);
	}

	/**
	 * Finds the correlating ExtractedField of an EStructuralFeature in an intermediate model.
	 */
	def static ExtractedField findField(EStructuralFeature feature, IntermediateModel model) {
		for (field : findType(feature.EContainingClass, model)?.fields) { // find correlating extracted class
			if (field.identifier.equals(feature.name)) { // find correlating extracted field
				return field
			}
		}
		return null
	}

	/**
	 * Finds the correlating ExtractedTypeParameter of an ETypeParameter in an intermediate model.
	 */
	def static ExtractedTypeParameter findTypeParameter(ETypeParameter eTypeParameter, EClass eClass, IntermediateModel model) {
		for (parameter : findType(eClass, model)?.typeParameters) {
			if (parameter.identifier.equals(eTypeParameter.name)) {
				return parameter
			}
		}
		return null
	}
}
