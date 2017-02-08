package jce.codegen

class WrapperGenerator {

	def String generate(String className, String factoryName, String wrapperPackage, String ecorePackage) '''
		package «wrapperPackage»
		
		import org.eclipse.xtend.lib.annotations.Delegate
		import «ecorePackage».«className»
		import «ecorePackage».«factoryName»
		
		/**
		 * Wrapper class for the class «className»
		 */
		class «className»Wrapper implements «className» {
			@Delegate
			private var «className» ecoreImplementation;
		
			new() {
				ecoreImplementation = «factoryName».eINSTANCE.create«className»();
			}
		}
	'''
}
