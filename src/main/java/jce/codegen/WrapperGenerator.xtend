package jce.codegen

import eme.generator.GeneratedEcoreMetamodel
import java.io.ByteArrayInputStream
import java.io.File
import jce.util.FolderRefresher
import jce.util.PathHelper
import jce.util.ProgressMonitorAdapter
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage

/** 
 * Creates and manages wrappers for the classes of the original Java project with is ecorified.
 * @author Timur Saglam
 */
final class WrapperGenerator {
	static final Logger logger = LogManager.getLogger(WrapperGenerator.getName)
	static final IProgressMonitor MONITOR = new ProgressMonitorAdapter(logger)
	static final PathHelper PACKAGE = new PathHelper(Character.valueOf('.').charValue)
	static final PathHelper PATH = new PathHelper(File.separatorChar)
	static final String SRC_FOLDER = "src"
	static final String WRAPPER_FOLDER = PATH.append(SRC_FOLDER, "wrappers")
	static IProject project

	private new() {
		// private constructor.
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def static void buildWrappers(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the wrapper generation...")
		WrapperGenerator::project = project
		createFolder(WRAPPER_FOLDER) // build wrapper base folder
		buildWrappers(metamodel.getRoot, "")
		FolderRefresher.refresh(project, SRC_FOLDER) // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the wrapper creation.
	 * @param ePackage is the current {@link EPackage} to create wrappers for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private static void buildWrappers(EPackage ePackage, String path) {
		if (!isEmpty(ePackage)) {
			createFolder(PATH.append(WRAPPER_FOLDER, path))
		}
		for (EClassifier eClassifier : ePackage.EClassifiers) { // for every classifier
			if (eClassifier instanceof EClass) { // if is class
				createXtendWrapper(path, eClassifier.getName) // create wrapper class
			}
		}
		for (EPackage eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildWrappers(eSubpackage, PATH.append(path, eSubpackage.getName)) // do the same
		}
	}

	/**
	 * Creates an {@link IFolder} in the project with a project relative path.
	 */
	def private static void createFolder(String path) {
		var IFolder folder = project.getFolder(path)
		if (!folder.exists) {
			folder.create(false, true, MONITOR)
		}
	}

	/**
	 * Creates a Xtend Wrapper in a package path with a specific name. 
	 */
	def private static void createXtendWrapper(String packagePath, String name) {
		val currentPackage = packagePath.replace(File.separatorChar, '.')
		var factoryName = '''«PACKAGE.nameOf(currentPackage)»Factory'''
		factoryName = factoryName.substring(0, 1).toUpperCase + factoryName.substring(1) // first letter upper case
		val content = wrapperContent(name, factoryName, currentPackage)
		createFile(packagePath, '''«name»Wrapper.xtend''', content)
	}

	/**
	 * Creates an IFile from a project relative path, a file name and creates the file content.
	 */
	def private static void createFile(String path, String name, String content) {
		var folder = project.getFolder(PATH.append(SRC_FOLDER, "wrappers", path))
		var file = folder.getFile(name)
		if (!file.exists) {
			val source = new ByteArrayInputStream(content.bytes)
			file.create(source, IResource.NONE, MONITOR)
			file.touch(MONITOR)
		}
	}

	/**
	 * Checks whether the EPackage is empty or not. An empty ePackage has no classifiers and only empty subpackages. 
	 */
	def private static boolean isEmpty(EPackage ePackage) {
		if (!ePackage.EClassifiers.empty) {
			return false // has classifier.
		} else {
			var empty = true
			for (EPackage eSubpackage : ePackage.ESubpackages) { // for every subpackage
				empty = empty && isEmpty(eSubpackage) // check if empty
			}
			return empty
		}
	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def private static String wrapperContent(String className, String factoryName, String currentPackage) '''
		package «PACKAGE.append("wrappers", currentPackage)»
		
		import org.eclipse.xtend.lib.annotations.Delegate
		import «PACKAGE.append("ecore", currentPackage)».«className»
		import «PACKAGE.append("ecore", currentPackage)».«factoryName»
		
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
