package jce.util

import org.eclipse.emf.ecore.EcorePackage
import java.util.Map
import org.eclipse.emf.ecore.EDataType
import java.util.HashMap
import org.eclipse.emf.ecore.EGenericType
import org.eclipse.emf.ecore.EStructuralFeature
import eme.model.IntermediateModel
import org.eclipse.emf.ecore.EClass

/**
 * Utility for the extraction of Java features from Ecore metamodel elements.
 */
final class EcoreToJavaUtil {

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/**
	 * Returns the Java name of an EGenericType.
	 */
	def static String getFeatureType(EGenericType eGenericType) {
		val token = "(expression: "
		val dataTypeMap = dataTypeMap
		var String type = eGenericType.toString
		type = type.substring(type.indexOf(token) + token.length, type.length - 1)
		for (dataType : dataTypeMap.keySet) {
			type = type.replaceAll(dataType.name, dataTypeMap.get(dataType))
		}
		/*
		 * Slightly botched way to get the Java representation through the toString() method of EGenericType.
		 * It returns the Java representation at the end of the string in brackets with the comment "expression:".
		 * Therefore we drop everything except the bracket content without the "expression:" comment.
		 * TODO (MEDIUM) implement this in a less botched way.
		 */
		return type
	}

	// TODO  (MEDIUM) maybe use this instead some time.
	def static String getFeatureType(EClass eClass, EStructuralFeature feature, IntermediateModel model) {
		for (field : model.getType(eClass.name).fields) { // find correlating extracted class
			if(field.identifier.equals(feature.name)) { // find correlating extracted field
				return field.fullType
			}
		}
		return null;
	}

	def private static Map<EDataType, String> getDataTypeMap() {
		val dataTypeMap = new HashMap<EDataType, String>
		dataTypeMap.put(EcorePackage.eINSTANCE.EBoolean, "boolean")
		dataTypeMap.put(EcorePackage.eINSTANCE.EByte, "byte")
		dataTypeMap.put(EcorePackage.eINSTANCE.EChar, "char")
		dataTypeMap.put(EcorePackage.eINSTANCE.getEDouble, "double")
		dataTypeMap.put(EcorePackage.eINSTANCE.EFloat, "float")
		dataTypeMap.put(EcorePackage.eINSTANCE.EInt, "int")
		dataTypeMap.put(EcorePackage.eINSTANCE.ELong, "long")
		dataTypeMap.put(EcorePackage.eINSTANCE.EShort, "short")
		return dataTypeMap
	}
}
