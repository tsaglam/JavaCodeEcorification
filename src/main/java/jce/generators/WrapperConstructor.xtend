package jce.generators

import java.util.LinkedList
import java.util.List
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors(PUBLIC_GETTER)
class WrapperConstructor { // TODO (HIGH) comment
	@Accessors(NONE) static final Logger logger = LogManager.getLogger(WrapperConstructor.getName)
	@Accessors(NONE) MethodDeclaration declaration
	@Accessors(NONE) List<SingleVariableDeclaration> parameters
	List<String> imports
	String content

	new(MethodDeclaration declaration) {
		this.declaration = declaration
		parameters = new LinkedList
		for (parameter : declaration.parameters) {
			addParameter(parameter)
		}
		content = buildContent
	}
	
		override toString() {
		return "WrapperConstructor with parameters " + parameters
	}

	def private dispatch void addParameter(Object parameter) {
		logger.error("Could not resolve parameter: " + parameter)
	}

	def private dispatch void addParameter(SingleVariableDeclaration parameter) {
		parameters.add(parameter)
	}

	def private String buildContent() '''
		
		new(«buildParameters») {
			super(«buildNames»)
			ecoreImplementation = instance
		}
	'''

	def private String buildParameters() {
		return String.join(", ", parameters.map[toString])
	}

	def private String buildNames() {
		return String.join(", ", parameters.map[name.identifier])
	}
}
