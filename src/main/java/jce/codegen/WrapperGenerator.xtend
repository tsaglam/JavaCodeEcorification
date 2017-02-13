package jce.codegen

import eme.generator.GeneratedEcoreMetamodel
import java.io.File
import java.io.FileWriter
import java.io.IOException
import jce.util.PathHelper
import jce.util.ProjectDirectories
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IWorkspaceRoot
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.Path
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage

/** 
 * Creates and manages wrappers for the classes of the orginal Java project with is ecorified.
 * @author Timur Saglam
 */
final class WrapperGenerator {
	static ProjectDirectories directories
	static final Logger logger = LogManager.getLogger(WrapperGenerator.getName)
	static final PathHelper PACKAGE = new PathHelper(Character.valueOf('.').charValue)
	static final PathHelper PATH = new PathHelper(File.separatorChar)

	private new() {
		// private constructor.
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def static void buildWrappers(GeneratedEcoreMetamodel metamodel, ProjectDirectories directories) {
		logger.info("Starting the wrapper class generation...")
		WrapperGenerator.directories = directories
		buildWrappers(metamodel.getRoot, "")
		refreshSourceFolder // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the wrapper creation.
	 * @param ePackage is the current {@link EPackage} to create wrappers for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private static void buildWrappers(EPackage ePackage, String path) {
		for (EClassifier eClassifier : ePackage.getEClassifiers) { // for every classifier
			if (eClassifier instanceof EClass) { // if is class
				createXtendWrapper(path, eClassifier.getName) // create wrapper class
			}
		}
		for (EPackage eSubpackage : ePackage.getESubpackages) { // for every subpackage
			buildWrappers(eSubpackage, PATH.append(path, eSubpackage.getName)) // do the same
		}
	}

	/** 
	 * Creates and Xtend wrapper class at a specific location with a specific name.
	 * @param packagePath is the path of the specific location.
	 * @param name is the name of the wrapper to generate.
	 */
	def private static void createXtendWrapper(String packagePath, String name) {
		var String filePath = PATH.append(directories.getSourceDirectory, "wrappers", packagePath, '''«name»Wrapper.xtend''')
		var String currentPackage = packagePath.replace(File.separatorChar, Character.valueOf('.').charValue)
		var String wrapperPackage = PACKAGE.append("wrappers", currentPackage)
		var String ecorePackage = PACKAGE.append("ecore", currentPackage)
		var String factoryName = '''«PACKAGE.nameOf(currentPackage)»Factory'''
		factoryName = factoryName.substring(0, 1).toUpperCase + factoryName.substring(1)
		var File file = new File(filePath)
		if (file.exists) {
			throw new IllegalArgumentException('''File already exists: «filePath»''')
		}
		file.getParentFile.mkdirs // ensure folder tree exists
		write(file, wrapperContent(name, factoryName, wrapperPackage, ecorePackage))
	}

	/** 
	 * Refreshes the source folder where the wrappers are generated in.
	 */
	def private static void refreshSourceFolder() {
		var IWorkspaceRoot root = ResourcesPlugin.getWorkspace.getRoot
		var IContainer folder = root.getContainerForLocation(new Path(directories.getSourceDirectory))
		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, null)
		} catch (CoreException exception) {
			logger.warn("Could not refresh source folder. Try that manually.", exception)
		}

	}

	/** 
	 * Writes a String to a {@link File}.
	 * @param file is the {@link File}.
	 * @param content is the content String.
	 */
	def private static void write(File file, String content) {
		try {
			file.createNewFile
			var FileWriter fileWriter = new FileWriter(file)
			fileWriter.write(content)
			fileWriter.flush
			fileWriter.close
		} catch (IOException exception) {
			exception.printStackTrace
		}

	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def static String wrapperContent(String className, String factoryName, String wrapperPackage, String ecorePackage) '''
		package «wrapperPackage»
		
		import org.eclipse.xtend.lib.annotations.Delegate
		import «ecorePackage».«className»
		import «ecorePackage».«factoryName»
		
		/**
		 * Wrapper class for the class «className»
		 */
		class «className»Wrapper implements «className» {
			@Delegate
			private var «className» ecoreImplementation
		
			new() {
				ecoreImplementation = «factoryName».eINSTANCE.create«className»()
			}
		}
	'''
}