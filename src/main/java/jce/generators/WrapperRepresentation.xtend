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
import org.eclipse.emf.ecore.InternalEObject
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.common.notify.Notifier
import jce.util.EcoreToJavaUtil

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
	final String ecoreInterface
	final String ecoreImplementation

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
		ecoreInterface = append(ECORE_PACKAGE.get, packageName, eClass.name)
		ecoreImplementation = append(ECORE_PACKAGE.get, packageName, "impl", eClass.name + "Impl")
		typeParameters = TypeParameterGenerator.generate(eClass.ETypeParameters, ecoreImplementation, project, properties)
		importDeclarations = new HashSet // add import declarations:
		if(superClass === null) {
			importDeclarations += InternalEObject.name
			importDeclarations += EObject.name
			importDeclarations += Notifier.name
		}
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
			
			«IF superClass === null»
				// Methods of InternalEObject must also be delegated to the wrapped class
				@DelegateExcept(«Notifier.simpleName», «EObject.simpleName»)
				protected var «InternalEObject.simpleName» internalEcoreImplementation
			«ENDIF»
		
			«constructors»
			
			«instanceMethod»
			
			«specialSetters»
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
				«IF superClass === null»
					internalEcoreImplementation = ecoreImplementation as «InternalEObject.simpleName»
				«ENDIF»
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
		import «ecoreInterface»
		«IF !eClass.abstract»
			import «append(ECORE_PACKAGE.get, packageName, factoryName)»
		«ENDIF»
		«IF superClass === null»
			import org.eclipse.emf.ecore.impl.MinimalEObjectImpl
			import org.eclipse.xtend.lib.annotations.Delegate
			import edu.kit.ipd.sdq.activextendannotations.DelegateExcept
		«ELSE»
			import edu.kit.ipd.sdq.activextendannotations.DelegateDeclared
			import «superClass»
		«ENDIF»
		«FOR importDeclaration : importDeclarations»
			import «importDeclaration»
		«ENDFOR»
			
		«IF eClass.EStructuralFeatures.exists[field | field.upperBound == -1]»
			import java.util.List
		«ENDIF»
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
	 * Returns a special setter for every field which was extracted using multiplicities.
	 */
	def private String getSpecialSetters() '''
		«FOR field : eClass.EStructuralFeatures»
			«IF field.upperBound == -1»				
				def protected void set«field.name.toFirstUpper» (List<«EcoreToJavaUtil.getFeatureType(field.EGenericType)»> «field.name») {
					get«field.name.toFirstUpper».clear
					get«field.name.toFirstUpper».addAll(«field.name»)
				}
					
			«ENDIF»
		«ENDFOR»
	''' // TODO (HIGH) imports

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
	def private String getTypeParameters() { // TODO (HIGH) remove duplicate code.
		if(typeParameters.empty) {
			return ""
		}
		val StringJoiner joiner = new StringJoiner(", ")
		for (parameter : typeParameters) {
			joiner.add(parameter.content)
		}
		return '''<«joiner»>'''
	}

	def private String getGenericArguments() { // TODO (HIGH) remove duplicate code.
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
