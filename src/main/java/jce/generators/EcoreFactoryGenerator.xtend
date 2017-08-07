package jce.generators

import jce.properties.EcorificationProperties

class EcoreFactoryGenerator extends ClassGenerator {

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		super(properties)
	} // TODO (HIGH) Add main method for class.

	def private String createFactoryContent(String currentPackage, String packageName) '''
		package «currentPackage»;
		
		import org.eclipse.emf.ecore.EFactory;
		
		/**
		 * <!-- begin-user-doc -->
		 * The <b>Factory</b> for the model.
		 * It provides a create method for each non-abstract class of the model.
		 * <!-- end-user-doc -->
		 * @see «currentPackage».«packageName»Package
		 * @generated
		 */
		public interface «packageName»Factory extends EFactory {
		    /**
		     * The singleton instance of the factory.
		     * <!-- begin-user-doc -->
		     * <!-- end-user-doc -->
		     * @generated
		     */
		    «packageName»Factory eINSTANCE = «currentPackage».impl.«packageName»FactoryImpl.init();
		
		
		«createFactoryMethod("TEST") /* TODO factory methods. */ »
		
		    /**
		     * Returns the package supported by this factory.
		     * <!-- begin-user-doc -->
		     * <!-- end-user-doc -->
		     * @return the package supported by this factory.
		     * @generated
		     */
		    «packageName»Package get«packageName»Package();
		
		} //«packageName»Factory
	'''

	def private String createFactoryMethod(String className) '''
		/**
				     * Returns a new object of class '<em>«className»</em>'.
				     * <!-- begin-user-doc -->
				     * <!-- end-user-doc -->
				     * @return a new object of class '<em>«className»</em>'.
				     * @generated
				     */
				    «className» create«className»();
				    
	'''
}
