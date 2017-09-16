package jce.generators

import java.util.List
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import org.apache.log4j.Logger
import org.apache.log4j.LogManager
import java.util.LinkedList

@Accessors(PUBLIC_GETTER)
class WrapperConstructor { // TODO complete
	@Accessors(NONE) static final Logger logger = LogManager.getLogger(WrapperConstructor.getName)
	@Accessors(NONE) MethodDeclaration declaration
	@Accessors(NONE) List<SingleVariableDeclaration> parameters
	List<String> imports
	String content

	new(MethodDeclaration declaration) {
		this.declaration = declaration
		parameters = new LinkedList
		for (parameter : declaration.parameters) {
			System.err.println(parameter) // TODO
			addParameter(parameter)
		}
	}

	def String buildContent() '''
		new() {
			super()
		}
		
	'''

	def String buildParameters() {
		var String content = ""
		for (parameter : parameters) {
			content += parameter + ", "
		}
		return content.substring(0, content.length - 2)
	}

	def String buildNames() {
		var String content = ""
		for (parameter : parameters) {
			content += parameter.name.identifier + ", "
		}
		return content.substring(0, content.length - 2)
	}

	def dispatch void addParameter(Object parameter) {
		logger.error("Could not resolve parameter: " + parameter);
	}

	def dispatch void addParameter(SingleVariableDeclaration parameter) {
		parameters.add(parameter)
	}
}
