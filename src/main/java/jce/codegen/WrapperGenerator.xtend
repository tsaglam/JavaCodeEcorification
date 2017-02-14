package jce.codegen

import eme.generator.GeneratedEcoreMetamodel
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import jce.util.PathHelper
import jce.util.ProgressMonitorAdapter
import jce.util.ProjectDirectories
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IWorkspaceRoot
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
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
	private static final IProgressMonitor MONITOR = new ProgressMonitorAdapter(logger)
	static final PathHelper PACKAGE = new PathHelper(Character.valueOf('.').charValue)
	static final PathHelper PATH = new PathHelper(File.separatorChar)
	static IProject project

	private new() {
		// private constructor.
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def static void buildWrappers(GeneratedEcoreMetamodel metamodel, IProject project, ProjectDirectories directories) {
		logger.info("Starting the wrapper class generation...")
		WrapperGenerator.project = project
		WrapperGenerator.directories = directories
		var IFolder folder = project.getFolder(PATH.append("src", File.separator, "wrappers"))
		folder.create(false, true, MONITOR)
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

	def private static void createXtendWrapper(String packagePath, String name) {
		val String currentPackage = packagePath.replace(File.separatorChar, Character.valueOf('.').charValue)
		val String wrapperPackage = PACKAGE.append("wrappers", currentPackage)
		val String ecorePackage = PACKAGE.append("ecore", currentPackage)
		var String factoryName = '''«PACKAGE.nameOf(currentPackage)»Factory'''
		factoryName = factoryName.substring(0, 1).toUpperCase + factoryName.substring(1) // first letter upper case
		val String content = wrapperContent(name, factoryName, wrapperPackage, ecorePackage)
		createFile(packagePath, '''«name»Wrapper.xtend''', content)
	}

	/**
	 * Creates an IFile and its IFolder from a package path, a file name and the file content.
	 */
	def private static void createFile(String packagePath, String name, String content) {
		var IFolder folder = project.getFolder(PATH.append("src", File.separator, "wrappers", packagePath))
		if (!folder.exists) { // TODO (HIGH) fix org.eclipse.e4.core.di.InjectionException: org.eclipse.core.internal.resources.ResourceException: Resource '/ProofOfConceptEcorified/src/wrappers' does not exist.
			folder.create(false, true, MONITOR)
		}
		var IFile file = folder.getFile(name)
		if (!file.exists()) {
			val InputStream source = new ByteArrayInputStream(content.bytes)
			file.create(source, IResource.NONE, MONITOR)
			file.touch(MONITOR)
		}
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
	 * Builds the content of a wrapper class.
	 */
	def private static String wrapperContent(String className, String factoryName, String wrapperPackage,
		String ecorePackage) '''
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