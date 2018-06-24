package jce.util

import java.util.List
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature

/** 
 * A utility class for searching elements in Ecore metamodels.
 * @author Heiko Klare, Timur Saglam
 */
final class EcoreUtil {
	static final PathHelper PATH = new PathHelper(Character.valueOf('.').charValue)

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/** 
	 * Finds a specific {@link EClass} in a {@link EPackage} and its subpackages.
	 * @param fullName is the fully qualified name of the desired {@link EClass}.
	 * @param ePackage is the {@link EPackage} which contains the {@link EClass}.
	 * @return the {@link EClass} or null if there is none with the specified name.
	 */
	def static EClass findEClass(String fullName, EPackage ePackage) {
		var String eClassName = PATH.getLastSegment(fullName) // get EClass name
		var EPackage parent = findESubpackage(PATH.cutLastSegment(fullName), ePackage) // search EPackage.
		if (parent !== null) { // if EPackage was found.
			for (EClassifier classifier : parent.getEClassifiers()) { // for every EClassifier:
				if (classifier instanceof EClass && isSame(classifier, eClassName)) {
					return (classifier as EClass) // search for the right EClass.
				}
			}
		}
		return null
	}

	/** 
	 * Finds a specific {@link EPackage} which is directly or indirectly a subpackage of a given {@link EPackage}.
	 * @param fullName is the fully qualified name of the desired {@link EPackage}.
	 * @param ePackage is the {@link EPackage} which contains the subpackage.
	 * @return the subpackage or null if there is none with the specified name.
	 */
	def static EPackage findESubpackage(String fullName, EPackage ePackage) {
		for (EPackage subpackage : ePackage.getESubpackages()) { // for every subpackage
			if (isSame(subpackage, PATH.getFirstSegment(fullName))) { // if is the right subpackage
				if (!PATH.hasMultipleSegments(fullName)) {
					return subpackage // return package if the are no more segments in the path
				} else {
					return findESubpackage(PATH.cutFirstSegment(fullName), subpackage) // search further for every segment
				}
			}
		}
		return null
	}

	/**
	 * Returns the package with the given fully qualified name, starting from the given root {@link EPackage}.
	 * @param rootPackage the root {@link EPackage} of a metamodel to start from
	 * @param fullyQualifiedName the fully qualified name of the package to search for
	 * @return the resolved {@link EPackage} or <code>null</code> if none was found
	 */
	def static EPackage findEPackage(EPackage ePackage, String fullName) {
		if (isSame(ePackage, fullName)) {
			return ePackage
		}
		return findESubpackage(PATH.cutFirstSegment(fullName), ePackage)
	}

	/** 
	 * Finds a specific {@link EStructuralFeature} in an {@link EClass}.
	 * @param fullName is the fully qualified name of the desired {@link EStructuralFeature}.
	 * @param eClass is the {@link EClass} which contains the {@link EStructuralFeature}.
	 * @return the {@link EStructuralFeature} or null if there is none with the specified name.
	 */
	def static EStructuralFeature findEStructuralFeature(String fullName, EClass eClass) {
		for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
			if (isSame(feature, fullName)) {
				return feature
			}
		}
		return null
	}

	/** 
	 * Finds a specific {@link EStructuralFeature} in an {@link EPackage}.
	 * @param fullName is the fully qualified name of the desired {@link EStructuralFeature}.
	 * @param eClassName is the fully qualified name of the {@link EClass} that contains the {@link EStructuralFeature}.
	 * @param ePackage ePackage is the {@link EPackage} which contains the {@link EClass} that contains the{@link EStructuralFeature}.
	 * @return the {@link EStructuralFeature} or null if there is none with the specified name.
	 */
	def static EStructuralFeature findEStructuralFeature(String fullName, String eClassName, EPackage ePackage) {
		var EClass eClass = findEClass(eClassName, ePackage)
		if (eClass !== null) { // if class was found
			return findEStructuralFeature(fullName, eClass) // search for feature
		}
		return null
	}
	
	/**
	 * Returns the list of names of all non-abstract {@link EClass}es in an {@link EPackage}.
	 */
	def static List<String> getClassNames(EPackage ePackage) {
		return ePackage.EClassifiers.filter(EClass).filter[!interface && !abstract].map[name].toList
	}

	/** 
	 * Compares the name of an ENamedElement with a String.
	 */
	def private static boolean isSame(ENamedElement element, String elementName) {
		return element.getName().equals(elementName)
	}
}
