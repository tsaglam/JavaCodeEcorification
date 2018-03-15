package jce.generators

import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.StringJoiner
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl
import org.eclipse.jdt.core.IJavaProject

import static jce.properties.TextProperty.ECORE_PACKAGE
import static jce.properties.TextProperty.FACTORY_SUFFIX
import static jce.properties.TextProperty.WRAPPER_PACKAGE
import static jce.properties.TextProperty.WRAPPER_PREFIX
import static jce.properties.TextProperty.WRAPPER_SUFFIX

/**
 * This class models a wrapper class which unifies an origin code type with its Ecore counterparts in the Ecore model
 * code and the metamodel.
 * @author Timur Saglam
 */
class WrapperRepresentation {
	extension PathHelper nameUtil
	extension EcorificationProperties properties

	final String packageName
	final String superClass
	final EClass eClass
	final String wrapperName
	final String factoryName
	final List<TypeParameterRepresentation> typeParameters
	final List<ConstructorRepresentation> wrapperConstructors
	final Set<String> importDeclarations

	/**
	 * Creates a new wrapper representation from an EClass and the EcorificationProperties. The EClass specifies which
	 * types are unified. The properties specify the employed naming scheme.
	 */
	new(EClass eClass, IJavaProject project, EcorificationProperties properties) {
		this.eClass = eClass
		this.properties = properties
		nameUtil = new PathHelper('.')
		packageName = getPackage(eClass)
		wrapperName = WRAPPER_PREFIX.get + eClass.name + WRAPPER_SUFFIX.get // name of the wrapper class
		factoryName = '''«PathHelper.capitalize(packageName.getLastSegment)»Factory«FACTORY_SUFFIX.get»'''
		superClass = getSuperClassName(eClass)
		wrapperConstructors = ConstructorGenerator.generate(superClass, project, properties)
		typeParameters = TypeParameterGenerator.generate(eClass.ETypeParameters, append(ECORE_PACKAGE.get, packageName, factoryName), project,
			properties)
		importDeclarations = new HashSet // add import declarations:
		wrapperConstructors.forEach[constructor|importDeclarations.addAll(constructor.imports)]
		typeParameters.forEach[parameter|importDeclarations.addAll(parameter.imports)]
	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def String getContent() '''
		package «append(WRAPPER_PACKAGE.get, packageName)»
		
		«imports»
		
		/**
		 * Unification class for the class «eClass.name»
		 */
		«IF eClass.abstract»abstract «ENDIF»class «wrapperName + getTypeParameters» extends «createSuperType(superClass)» implements «eClass.name + genericArguments» {
			
			«delegateAnnotation»
			protected var «eClass.name + genericArguments» ecoreImplementation
			
				«constructors»
			
			«instanceMethod»
		}
	'''

	/**
	 * Returns the name of the wrapper.
	 */
	def String getName() {
		return wrapperName
	}

	/**
	 * Returns the name of the package of the wrapper.
	 */
	def String getPackage() {
		return packageName
	}

	/**
	 * Builds the super type declaration of a wrapper from a String that is either the super type or null.
	 */
	def private String createSuperType(String superClass) {
		if(superClass === null) {
			return append(typeof(MinimalEObjectImpl).simpleName, typeof(MinimalEObjectImpl.Container).simpleName)
		}
		return superClass.getLastSegment
	}

	/**
	 * Creates the constructors depending on the super class.
	 */
	def private String getConstructors() '''
		«IF superClass === null || wrapperConstructors.empty»
			new() {
				ecoreImplementation = instance
			}
		«ELSE»
			«FOR constructor : wrapperConstructors SEPARATOR blankLine»
				«constructor.content»
			«ENDFOR»
		«ENDIF»
	'''

	/**
	 * Creates the delegate annotation.
	 */
	def private String getDelegateAnnotation() '''
		«IF superClass === null»
			@Delegate
		«ELSE»
			@DelegateDeclared
		«ENDIF»
	'''

	/**
	 * Creates the import declarations depending on the super class.
	 */
	def private String getImports() '''
		import «append(ECORE_PACKAGE.get, packageName, eClass.name)»
		«IF !eClass.abstract»
			import «append(ECORE_PACKAGE.get, packageName, factoryName)»
		«ENDIF»
		«IF superClass === null»
			import org.eclipse.emf.ecore.impl.MinimalEObjectImpl
			import org.eclipse.xtend.lib.annotations.Delegate
		«ELSE»
			import edu.kit.ipd.sdq.activextendannotations.DelegateDeclared
			import «superClass»
		«ENDIF»
		«FOR importDeclaration : importDeclarations»
			import «importDeclaration»
		«ENDFOR»
	'''

	/**
	 * Creates the instance template method.
	 */
	def private String getInstanceMethod() '''
		«IF eClass.abstract»
			«methodKeyword» protected abstract «eClass.name + genericArguments» getInstance()
		«ELSE»
			«methodKeyword» protected «eClass.name + genericArguments» getInstance() {
				return «factoryName».eINSTANCE.create«eClass.name»
			}
		«ENDIF»
	'''

	/**
	 * 	Starts a method declaration. Returns either "def" or "override" depending on whether the EClass has a superclass.
	 */
	def private String getMethodKeyword() '''«IF superClass === null»def«ELSE»override«ENDIF»'''

	/**
	 * Returns the fully qualified name of the super class of an EClass.
	 */
	def private String getSuperClassName(EClass eClass) {
		val EClass superType = getSuperClass(eClass)
		if(superType !== null) {
			return append(getPackage(superType), superType.name)
		}
		return null
	}

	/**
	 * Generates the String of type type parameters with their respective bounds, e.g. "<T extends List<EString> & IFace<List<EString>>>"
	 */
	def private String getTypeParameters() { // TODO (MEDIUM) remove duplicate code.
		if(typeParameters.empty) {
			return ""
		}
		val StringJoiner joiner = new StringJoiner(", ")
		for (parameter : typeParameters) {
			joiner.add(parameter.content)
		}
		return '''<«joiner»>'''
	}

	def private String getGenericArguments() { // TODO (MEDIUM) remove duplicate code.
		if(typeParameters.empty) {
			return ""
		}
		val StringJoiner joiner = new StringJoiner(", ")
		for (parameter : typeParameters) {
			joiner.add(parameter.name)
		}
		return '''<«joiner»>'''
	}

	/**
	 * Returns super class of an EClass or null if it has none.
	 */
	def private EClass getSuperClass(EClass eClass) {
		for (superType : eClass.ESuperTypes) {
			if(!superType.interface) {
				return superType
			}
		}
		return null
	}

	/**
	 * Returns the full package path of an EClass. 
	 */
	def private String getPackage(EClass eClass) {
		var String package = ""
		var EPackage current = eClass.EPackage
		while(current !== null) { // iterate through package hierarchy
			package = append(current.name, package) // concatenate package with super package name
			current = current.ESuperPackage
		}
		return package.cutFirstSegment // cut Ecore package name
	}

	/**
	 * Generates a blank line in a template.
	 */
	def private String getBlankLine() '''
		
	'''
}
