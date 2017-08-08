package jce.generators

import java.io.File
import java.util.List
import jce.properties.EcorificationProperties
import org.eclipse.core.resources.IProject

/**
 * Generator class for the generation of Ecore factory implementation classes.
 * @author Timur Saglam
 */
class EFactoryImplementationGenerator extends ClassGenerator {

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		super(properties)
	}

	/**
	 * Creates a Ecore Factory in a package path with a specific name. 
	 */
	def public void create(String path, List<String> packageTypes, IProject project) {
		val currentPackage = path.replace(File.separatorChar, '.') // path to package declaration
		val packageName = pathUtil.getLastSegment(currentPackage)
		val content = createFactoryContent(currentPackage, packageName, packageTypes)
		createClass(path, '''«packageName»FactoryImpl.java''', content, project)
		monitor.subTask(''' Created «packageName»FactoryImpl.java''') // detailed logging
	}

	/**
	 * Creates the content of an Ecore factory.
	 */ // TODO (HIGH) Customize the factory code to create origin code.
	def private String createFactoryContent(String currentPackage, String packageName, List<String> packageTypes) '''
		package «currentPackage».impl;
		
		import org.eclipse.emf.ecore.EClass;
		import org.eclipse.emf.ecore.EObject;
		import org.eclipse.emf.ecore.EPackage;
		import org.eclipse.emf.ecore.impl.EFactoryImpl;
		import org.eclipse.emf.ecore.plugin.EcorePlugin;
		
		import «currentPackage».«packageName»Factory;
		import «currentPackage».«packageName»Package;
		«FOR type : packageTypes»
			import «currentPackage».«type»;
		«ENDFOR»
		
		/**
		 * An implementation of the model <b>Factory</b>.
		 * @generated
		 */
		public class «packageName»FactoryImpl extends EFactoryImpl implements «packageName»Factory {
		    /**
		     * Creates the default factory implementation.
		     * <!-- begin-user-doc -->
		     * <!-- end-user-doc -->
		     * @generated
		     */
		    public static «packageName»Factory init() {
		        try {
		            «packageName»Factory the«packageName»Factory = («packageName»Factory)EPackage.Registry.INSTANCE.getEFactory(«packageName»Package.eNS_URI);
		            if (the«packageName»Factory != null) {
		                return the«packageName»Factory;
		            }
		        }
		        catch (Exception exception) {
		            EcorePlugin.INSTANCE.log(exception);
		        }
		        return new «packageName»FactoryImpl();
		    }
		
		    /**
		     * Creates an instance of the factory.
		     * @generated
		     */
		    public «packageName»FactoryImpl() {
		        super();
		    }
		
		    /**
		     * @generated
		     */
		    @Override
		    public EObject create(EClass eClass) {
		        switch (eClass.getClassifierID()) {
		            case «packageName»Package.«packageName.toUpperCase»: return create«packageName»();
		            case «packageName»Package.«packageName.toUpperCase»_CONTAINER: return create«packageName»Container();
		            «FOR type : packageTypes»
		            	case «packageName»Package.«constantName(type)»: return createMember();
		            «ENDFOR»
		            default:
		                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		        }
		    }
		
		«FOR type : packageTypes»
			«createFactoryMethod(type)»
		«ENDFOR»
			/**
		 * @generated
		 */
		 public «packageName»Package get«packageName»Package() {
		     return («packageName»Package)getEPackage();
		 }
		
		    /**
		     * @deprecated
		     * @generated
		     */
		    @Deprecated
		    public static «packageName»Package getPackage() {
		        return «packageName»Package.eINSTANCE;
		    }
		    
		} //«packageName»FactoryImpl
	'''

	/**
	 * Creates the content of an Ecore factory method.
	 */
	def private String createFactoryMethod(String className) '''
		/**
		 * @generated
		 */
		public «className» create«className»() {
			«className»Impl «className.toLowerCase» = new «className»Impl();
			return «className.toLowerCase»;
		}
	'''

	/**
	 * Makes a type a name a constant name (MyType => MY_TYPE)
	 */
	def private String constantName(String typeName) {
		var constantName = "" // empty result string
		for (letter : typeName.toCharArray) {
			if (Character.isUpperCase(letter)) {
				constantName += '_' + letter // separate camel case words
			} else {
				constantName += Character.toUpperCase(letter) // letter to upper case
			}
		}
		return constantName
	}
}
