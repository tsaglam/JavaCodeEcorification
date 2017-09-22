package jce.generators

import java.util.LinkedList
import java.util.List
import jce.util.jdt.TypeUtil
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * This class models a constructor of a wrapper class.
 * @author Timur Saglam
 */
@Accessors(PUBLIC_GETTER)
class WrapperConstructor {
	@Accessors(NONE) static final Logger logger = LogManager.getLogger(WrapperConstructor.getName)
	@Accessors(NONE) MethodDeclaration declaration
	@Accessors(NONE) List<SingleVariableDeclaration> parameters
	List<String> imports
	String content

	/**
	 * Basic constructor, creates a wrapper constructor from a MethodDeclaration of a origin code constructor and the
	 * correlating compilation unit. 
	 */
	new(MethodDeclaration declaration, ICompilationUnit unit) {
		this.declaration = declaration
		parameters = new LinkedList
		for (parameter : declaration.parameters) {
			addParameter(parameter)
		}
		content = buildContent
		buildImports(unit)
	}

	override toString() {
		return class.name + parameters
	}

	/**
	 * Dispatch method for dealing with the raw list of parameters. This method should never be called.
	 */
	def private dispatch void addParameter(Object parameter) {
		logger.error("Could not resolve parameter: " + parameter)
	}

	/**
	 * Dispatch method for dealing with the raw list of parameters. This method should be called for every parameter.
	 */
	def private dispatch void addParameter(SingleVariableDeclaration parameter) {
		parameters.add(parameter)
	}

	/**
	 * Builds the Xtend code fragment of this constructor.
	 */
	def private String buildContent() '''
		new(«buildParameters») {
			super(«buildNames»)
			ecoreImplementation = instance
		}
	'''

	/** 
	 * Builds the list of types that need to be imported to use the constructors.
	 */
	def private void buildImports(ICompilationUnit unit) {
		imports = new LinkedList
		for (import : unit.imports) { // for every import
			for (parameter : parameters) { // if is needed for parameter
				if(import.elementName.endsWith(TypeUtil.getTypeName(parameter.type))) {
					imports.add(import.elementName) // add to import string list.
				}
			}
		}
	}

	/**
	 * Builds the list of parameters.
	 */
	def private String buildParameters() {
		return String.join(", ", parameters.map[toString])
	}

	/**
	 * Builds the list of parameter names.
	 */
	def private String buildNames() {
		return String.join(", ", parameters.map[name.identifier])
	}
}
