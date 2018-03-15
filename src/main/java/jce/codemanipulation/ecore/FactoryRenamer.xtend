package jce.codemanipulation.ecore

import eme.generator.GeneratedEcoreMetamodel
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import org.eclipse.jdt.core.JavaModelException

import static extension jce.util.PathHelper.capitalize

/**
 * Abstract code manipulator that renames the original Ecore factory interfaces.
 * @author Timur Saglam
 */
class FactoryRenamer extends AbstractFactoryRenamer {
	extension PathHelper nameUtil = super.nameUtil

	/**
	 * Simple constructor, sets the metamodel and the properties.
	 * @param metamodel is the extracted Ecore metamodel.
	 * @param properties are the {@link EcorificationProperties}.
	 */
	new(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
		super(metamodel, properties)
	}

	/**
	 * Checks whether a Java type has the name of an Ecore factory interface.
	 * @param fullName is the fully qualified name of the java type.
	 * @return true if it has a factory name.
	 * @throws JavaModelException if there is a problem with the JDT API.
	 */
	override protected hasFactoryName(String fullName) throws JavaModelException {
		val packageName = fullName.cutLastSegment.lastSegment
		if(fullName.endsWith(packageName.capitalize + "Factory")) { // if has factory name
			return !fullName.isInMetamodel // check if not in metamodel
		}
		return false; // Does not have Ecore interface name and package
	}
}
