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

	/**
	 * Returns the package with the given fully qualified name, starting from the given root {@link EPackage}.
	 * 
	 * @param rootPackage the root {@link EPackage} of a metamodel to start from
	 * @param fullyQualifiedName the fully qualified name of the package to search for
	 * @return the resolved {@link EPackage} or <code>null</code> if none was found
	 */
	def static EPackage findPackage(EPackage rootPackage, String fullyQualifiedName) { // TODO (HIGH) duplicate code with MetamodelSearchar.findEPackage()?
		val pathHelper = new PathHelper(".");
		if (rootPackage.ESuperPackage !== null || rootPackage.name != pathHelper.getFirstSegment(fullyQualifiedName)) {
			return null;
		}
		var relativeName = pathHelper.cutFirstSegment(fullyQualifiedName, false);
		var currentPackage = rootPackage;
		while (!relativeName.empty) {
			val currentPackageName = pathHelper.getFirstSegment(relativeName);
			currentPackage = currentPackage.ESubpackages.findFirst[name == currentPackageName];
			if (currentPackage === null) {
				return null;
			}
			relativeName = pathHelper.cutFirstSegment(relativeName, false);
		}
		return currentPackage;
	}

}
