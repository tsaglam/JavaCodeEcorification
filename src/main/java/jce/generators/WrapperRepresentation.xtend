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
import eme.model.IntermediateModel
import org.eclipse.emf.ecore.EStructuralFeature
import jce.util.IntermediateModelUtil

/**
 * This class models a wrapper class which unifies an origin code type with its Ecore counterparts in the Ecore model
 * code and the metamodel.
 * @author Timur Saglam
 */
class WrapperRepresentation {
	extension PathHelper nameUtil
	extension EcorificationProperties properties

	String packageName
	String superClass
	final EClass eClass
	String wrapperName
	String factoryName
	List<TypeParameterRepresentation> typeParameters
	List<ConstructorRepresentation> wrapperConstructors
	Set<String> importDeclarations
	String ecoreInterface
	String ecoreImplementation
	final IntermediateModel model

	/**
	 * Creates a new wrapper representation from an EClass and the EcorificationProperties. The EClass specifies which
	 * types are unified. The properties specify the employed naming scheme.
	 */
	new(EClass eClass, IJavaProject project, IntermediateModel model, EcorificationProperties properties) {
		this.eClass = eClass
		this.model = model
		this.properties = properties
		nameUtil = new PathHelper('.')
		createContent(project) // creates the important parts
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
		«IF eClass.abstract»abstract «ENDIF»class «wrapperName + getParameters(true)» extends «createSuperType(superClass)» implements «eClass.name + getParameters(false)» {
			
			«delegateAnnotation»
			protected var «eClass.name + getParameters(false)» ecoreImplementation
			
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

	def private createContent(IJavaProject project) {
		packageName = getPackage(eClass)
		wrapperName = WRAPPER_PREFIX.get + eClass.name + WRAPPER_SUFFIX.get // name of the wrapper class
		factoryName = '''«PathHelper.capitalize(packageName.getLastSegment)»Factory«FACTORY_SUFFIX.get»'''
		superClass = getSuperClassName(eClass)
		wrapperConstructors = ConstructorGenerator.generate(superClass, project, properties)
		ecoreInterface = append(ECORE_PACKAGE.get, packageName, eClass.name)
		ecoreImplementation = append(ECORE_PACKAGE.get, packageName, "impl", eClass.name + "Impl")
		typeParameters = TypeParameterGenerator.generate(eClass.ETypeParameters, ecoreImplementation, project, properties)
		importDeclarations = new HashSet // add import declarations:
		if (superClass === null) {
			importDeclarations += InternalEObject.name
			importDeclarations += EObject.name
			importDeclarations += Notifier.name
		}
		wrapperConstructors.forEach[constructor|importDeclarations.addAll(constructor.imports)]
		typeParameters.forEach[parameter|importDeclarations.addAll(parameter.imports)]
	}

	/**
	 * Builds the super type declaration of a wrapper from a String that is either the super type or null.
	 */
	def private String createSuperType(String superClass) {
		if (superClass === null) {
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
			«methodKeyword» protected abstract «eClass.name + getParameters(false)» getInstance()
		«ELSE»
			«methodKeyword» protected «eClass.name + getParameters(false)» getInstance() {
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
		«FOR field : eClass.EStructuralFeatures SEPARATOR blankLine»
			«IF field.upperBound == -1»
				def protected void set«field.name.toFirstUpper» (List<«getGenericArguments(field)»> «field.name») {
					get«field.name.toFirstUpper».clear
					get«field.name.toFirstUpper».addAll(«field.name»)
				}
			«ENDIF»
		«ENDFOR»
	'''

	def private String getGenericArguments(EStructuralFeature feature) {
		var String result = ""
		for (argument : IntermediateModelUtil.findField(feature, model).genericArguments) {
			result += argument.typeString
		}
		return result
	}

	/**
	 * Returns the fully qualified name of the super class of an EClass.
	 */
	def private String getSuperClassName(EClass eClass) {
		val EClass superType = getSuperClass(eClass)
		if (superType !== null) {
			return append(getPackage(superType), superType.name)
		}
		return null
	}

	/**
	 * Generates the String of type parameters.
	 * @param includeBounds determines whether the String contains the parameters with their respective bounds, e.g. "<T extends List<EString> & IFace<List<EString>>>"
	 */
	def private String getParameters(boolean includeBounds) {
		if (typeParameters.empty) {
			return "" // no type parameters at all
		}
		val StringJoiner joiner = new StringJoiner(", ") // join parameters with commas
		for (parameter : typeParameters) {
			if (includeBounds) {
				joiner.add(parameter.content) // name and bounds with their arguments
			} else {
				joiner.add(parameter.name) // only name
			}
		}
		return '''<«joiner»>''' // return parameters with generic brackets
	}

	/**
	 * Returns super class of an EClass or null if it has none.
	 */
	def private EClass getSuperClass(EClass eClass) {
		for (superType : eClass.ESuperTypes) {
			if (!superType.interface) {
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
		while (current !== null) { // iterate through package hierarchy
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
