package jce.generators

import java.io.File
import java.util.List
import jce.properties.EcorificationProperties
import org.eclipse.core.resources.IProject

/**
 * Generator class for the generation of Ecore factory interfaces.
 * @author Timur Saglam
 */
class EFactoryGenerator extends ClassGenerator {

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
		val packageName = packageUtil.getLastSegment(currentPackage).toFirstUpper
		val content = createFactoryContent(currentPackage, packageName, packageTypes)
		createClass(path, '''«packageName»Factory.java''', content, project)
		monitor.subTask(''' Created «packageName»Factory.java''') // detailed logging
	}

	/**
	 * Creates the content of an Ecore factory.
	 */
	def private String createFactoryContent(String currentPackage, String packageName, List<String> packageTypes) '''
		package «currentPackage»;
		
		import org.eclipse.emf.ecore.EFactory;
		
		/**
		 * The <b>Factory</b> for the model.
		 * It provides a create method for each non-abstract class of the model.
		 * @see «currentPackage».«packageName»Package
		 * @generated
		 */
		public interface «packageName»Factory extends EFactory {
		    /**
		     * The singleton instance of the factory.
		     * @generated
		     */
		    «packageName»Factory eINSTANCE = «currentPackage».impl.«packageName»FactoryImpl.init();
		«FOR type : packageTypes»
			«createFactoryMethod(type)»
		«ENDFOR»
		
		    /**
		 	 * Returns the package supported by this factory.
			 * @return the package supported by this factory.
			 * @generated
			 */
			«packageName»Package get«packageName»Package();
		
		} //«packageName»Factory
	'''

	/**
	 * Creates the content of an Ecore factory method.
	 */
	def private String createFactoryMethod(String className) '''
		
				/**
				 * Returns a new object of class '<em>«className»</em>'.
				 * @return a new object of class '<em>«className»</em>'.
				 * @generated
				 */
				«className» create«className»();
				    
	'''
}
