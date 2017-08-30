package jce.generators

import jce.properties.EcorificationProperties
import jce.util.PathHelper
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage

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

	/**
	 * Creates a new wrapper representation from an EClass and the EcorificationProperties. The EClass specifies which
	 * types are unified. The properties specify the employed naming scheme.
	 */
	new(EClass eClass, EcorificationProperties properties) {
		this.eClass = eClass
		this.properties = properties
		nameUtil = new PathHelper('.')
		packageName = getPackage(eClass)
		wrapperName = WRAPPER_PREFIX.get + PathHelper.capitalize(eClass.name) + WRAPPER_SUFFIX.get // name of the wrapper class
		factoryName = '''«PathHelper.capitalize(packageName.getLastSegment)»Factory«FACTORY_SUFFIX.get»'''
		superClass = getSuperClass(eClass)
	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def String getContent() '''
		package «nameUtil.append(WRAPPER_PACKAGE.get, packageName)»
		
		import «nameUtil.append(ECORE_PACKAGE.get, packageName)».«eClass.name»
		import «nameUtil.append(ECORE_PACKAGE.get, packageName)».«factoryName»
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
		
			«IF eClass.abstract»
				new() {
					ecoreImplementation = instance
				}
				
				def protected abstract «eClass.name» getInstance()
			«ELSE»
				override protected getInstance() {
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
	 * Builds the super type declaration of a wrapper from a String that is either the super type or null.
	 */
	def private String createSuperType(String superClass) {
		if(superClass === null) {
			return "MinimalEObjectImpl.Container"
		}
		return nameUtil.getLastSegment(superClass)
	}

	/**
	 * Returns the fully qualified name of the super class of an EClass.
	 */
	def private String getSuperClass(EClass eClass) {
		for (superType : eClass.ESuperTypes) {
			if(!superType.interface) {
				return append(getPackage(superType), superType.name)
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
		while(current !== null) {
			package = append(current.name, package)
			current = current.ESuperPackage
		}
		return package.cutFirstSegment
	}
}
