package jce.generators

import java.util.LinkedList
import java.util.List
import jce.properties.EcorificationProperties
import jce.util.logging.MonitorFactory
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.emf.ecore.ETypeParameter
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType

/**
 * Generator class for the generation of type parameter representations for the wrapper classes.
 */
final class TypeParameterGenerator {
	static final Logger logger = LogManager.getLogger(TypeParameterGenerator.name)

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/**
	 * Builds the type parameter representations from a list of ETypeParameters and the name of the correlating Ecore interface of the wrapper.
	 */
	def static List<TypeParameterRepresentation> generate(List<ETypeParameter> eTypeParameters, String ecoreInterface, IJavaProject project,
		EcorificationProperties properties) {
		val parameters = new LinkedList<TypeParameterRepresentation>;
		val IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties)
		val IType type = project.findType(ecoreInterface, monitor)
		if(type === null || type.compilationUnit === null) {
			logger.error("Could not get compilation unit of " + ecoreInterface)
		} else {
			for (eTypeParameter : eTypeParameters) { // create type parameter representations:
				parameters.add(new TypeParameterRepresentation(eTypeParameter, type.compilationUnit))
			}
		}
		return parameters
	}

}
