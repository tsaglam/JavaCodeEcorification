package jce.generators

import jce.properties.EcorificationProperties
import jce.util.PathHelper
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl

import static jce.properties.TextProperty.ECORE_PACKAGE
import static jce.properties.TextProperty.FACTORY_SUFFIX
import static jce.properties.TextProperty.WRAPPER_PACKAGE
import static jce.properties.TextProperty.WRAPPER_PREFIX
import static jce.properties.TextProperty.WRAPPER_SUFFIX
import eme.model.IntermediateModel
import eme.model.ExtractedType

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
	final ExtractedType intermediateModelType;

	/**
	 * Creates a new wrapper representation from an EClass and the EcorificationProperties. The EClass specifies which
	 * types are unified. The properties specify the employed naming scheme.
	 */
	new(EClass eClass, IntermediateModel model, EcorificationProperties properties) { // TODO (HIGH) clean code.
		this.eClass = eClass
		this.properties = properties
		nameUtil = new PathHelper('.')
		packageName = getPackage(eClass)
		wrapperName = WRAPPER_PREFIX.get + eClass.name + WRAPPER_SUFFIX.get // name of the wrapper class
		factoryName = '''«PathHelper.capitalize(packageName.getLastSegment)»Factory«FACTORY_SUFFIX.get»'''
		superClass = getSuperClassName(eClass)
		intermediateModelType = model.getType(append(packageName, eClass.name));
		if(intermediateModelType === null) {
			throw new IllegalArgumentException("Could not find EClass counterpart in intermediate model.")
		}
	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def String getContent() '''
		package «append(WRAPPER_PACKAGE.get, packageName)»
		
		import «append(ECORE_PACKAGE.get, packageName)».«eClass.name»
		«IF !eClass.abstract»
			import «append(ECORE_PACKAGE.get, packageName)».«factoryName»
		«ENDIF»
		«IF superClass === null»
			import org.eclipse.emf.ecore.impl.MinimalEObjectImpl
			import org.eclipse.xtend.lib.annotations.Delegate
		«ELSE»
			import edu.kit.ipd.sdq.activextendannotations.DelegateDeclared
			import «superClass»
		«ENDIF»
		
		/**
		 * Unification class for the class «eClass.name»
		 */
		«IF eClass.abstract»abstract «ENDIF»class «wrapperName» extends «createSuperType(superClass)» implements «eClass.name» {
			«IF superClass === null»
				@Delegate
			«ELSE»
				@DelegateDeclared
			«ENDIF»
			protected var «eClass.name» ecoreImplementation
			
			new() {
				ecoreImplementation = instance
			}
			
			«IF eClass.abstract»
				«getMethodKeyword()» protected abstract «eClass.name» getInstance()
			«ELSE»
				«getMethodKeyword()» protected «eClass.name» getInstance() {
					return «factoryName».eINSTANCE.create«eClass.name»
				}
			«ENDIF»
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
}
