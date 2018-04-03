package jce.util

import java.util.List
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EClass

/**
 * Utility methods to extract information from an {@link EPackage}.
 * @author Heiko Klare
 */
class EPackageUtil {
	/**
	 * Returns the list of names of all non-abstract {@link EClass}es in an {@link EPackage}.
	 */
	def static List<String> getClassNames(EPackage ePackage) {
		return ePackage.EClassifiers.filter(EClass).filter[!interface && !abstract].map[name].toList
	}

}