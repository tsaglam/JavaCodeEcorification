package jce.generators

import eme.generator.GeneratedEcoreMetamodel
import java.io.ByteArrayInputStream
import java.io.File
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import jce.util.ResourceRefresher
import jce.util.logging.MonitorFactory
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage

import static jce.properties.TextProperty.ECORE_PACKAGE
import static jce.properties.TextProperty.WRAPPER_PACKAGE
import static jce.properties.TextProperty.WRAPPER_PREFIX
import static jce.properties.TextProperty.WRAPPER_SUFFIX

/** 
 * Creates and manages wrappers for the classes of the original Java project with is ecorified.
 * @author Timur Saglam
 */
final class WrapperGenerator {
	static final Logger logger = LogManager.getLogger(WrapperGenerator.getName)
	static final String SRC_FOLDER = "src" // TODO (HIGH) use source folder property
	final IProgressMonitor monitor
	final PathHelper packageUtil
	final PathHelper pathUtil
	final String wrapperFolder
	IProject project
	final EcorificationProperties properties

	new(EcorificationProperties properties) {
		this.properties = properties
		monitor = MonitorFactory.createProgressMonitor(logger, properties)
		packageUtil = new PathHelper(Character.valueOf('.').charValue)
		pathUtil = new PathHelper(File.separatorChar)
		wrapperFolder = pathUtil.append(SRC_FOLDER, properties.get(WRAPPER_PACKAGE))
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def void buildWrappers(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the wrapper generation...")
		this.project = project
		createFolder(wrapperFolder) // build wrapper base folder
		buildWrappers(metamodel.root, "")
		ResourceRefresher.refresh(project, SRC_FOLDER) // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the wrapper creation.
	 * @param ePackage is the current {@link EPackage} to create wrappers for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private void buildWrappers(EPackage ePackage, String path) {
		if (containsEClass(ePackage)) { // avoids empty folders
			createFolder(pathUtil.append(wrapperFolder, path))
		}
		for (eClassifier : ePackage.EClassifiers) { // for every classifier
			if (eClassifier instanceof EClass) { // if is EClass
				if (!eClassifier.interface) { // if is not interface
					createXtendWrapper(path, eClassifier.name, getSuperClass(eClassifier)) // create wrapper class
				}
			}
		}
		for (eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildWrappers(eSubpackage, pathUtil.append(path, eSubpackage.name)) // do the same
		}
	}

	/**
	 * Creates an {@link IFolder} in the project with a project relative path.
	 */
	def private void createFolder(String path) {
		var IFolder folder = project.getFolder(path)
		if (!folder.exists) {
			folder.create(false, true, monitor)
		}
	}

	/**
	 * Creates a Xtend Wrapper in a package path with a specific name. 
	 */
	def private void createXtendWrapper(String path, String name, String superClass) {
		val currentPackage = path.replace(File.separatorChar, '.') // path to package declaration
		var factoryName = '''«PathHelper.capitalize(packageUtil.getLastSegment(currentPackage))»Factory''' // name of the ecore factory of the package
		val className = properties.get(WRAPPER_PREFIX) + PathHelper.capitalize(name) + properties.get(WRAPPER_SUFFIX) // name of the wrapper class
		val content = wrapperContent(name, className, factoryName, currentPackage, superClass) // content of the class
		createFile(path, '''«className».xtend''', content)
		monitor.subTask(''' Created «currentPackage».«className».xtend''') // detailed logging
	}

	/**
	 * Creates an IFile from a project relative path, a file name and creates the file content.
	 */
	def private void createFile(String path, String name, String content) {
		var folder = project.getFolder(pathUtil.append(SRC_FOLDER, properties.get(WRAPPER_PACKAGE), path))
		var file = folder.getFile(name)
		if (!file.exists) {
			val source = new ByteArrayInputStream(content.bytes)
			file.create(source, IResource.NONE, monitor)
			file.touch(monitor)
		}
	}

	/**
	 * Returns the fully qualified name of the super class of an EClass.
	 */
	def private String getSuperClass(EClass eClass) {
		for (superType : eClass.ESuperTypes) {
			if (!superType.interface) {
				return packageUtil.append(getPackage(superType), superType.name)
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
		while (current !== null) {
			package = packageUtil.append(current.name, package)
			current = current.ESuperPackage
		}
		return packageUtil.cutFirstSegment(package)
	}

	/**
	 * Checks whether the EPackage contains an EClass. An empty EPackage has no classifiers and only empty subpackages. 
	 */
	def private boolean containsEClass(EPackage ePackage) {
		var containsEClass = false
		for (EClassifier eClassifier : ePackage.EClassifiers) { // for every classifier
			containsEClass = containsEClass || eClassifier instanceof EClass // check if is EClass
		}
		for (EPackage eSubpackage : ePackage.ESubpackages) { // for every subpackage
			containsEClass = containsEClass || containsEClass(eSubpackage) // check if empty
		}
		return containsEClass
	}

	/**
	 * Builds the content of a wrapper class.
	 */
	def private String wrapperContent(String className, String wrapperName, String factoryName, String currentPackage,
		String superClass) '''
		package «packageUtil.append(properties.get(WRAPPER_PACKAGE), currentPackage)»
		
		import «packageUtil.append(properties.get(ECORE_PACKAGE), currentPackage)».«className»
		import «packageUtil.append(properties.get(ECORE_PACKAGE), currentPackage)».«factoryName»
		«IF superClass === null»
			import org.eclipse.emf.ecore.impl.MinimalEObjectImpl
			import org.eclipse.xtend.lib.annotations.Delegate
		«ELSE»
			import edu.kit.ipd.sdq.activextendannotations.DelegateDeclared
			import «superClass»
		«ENDIF»
		
		/**
		 * Unification class for the class «className»
		 */
		class «wrapperName» extends «IF superClass === null»MinimalEObjectImpl.Container«ELSE»«packageUtil.getLastSegment(superClass)»«ENDIF» implements «className» {
			«IF superClass === null»
				@Delegate
			«ELSE»
				@DelegateDeclared
			«ENDIF»
			private var «className» ecoreImplementation
		
			new() {
				ecoreImplementation = «factoryName».eINSTANCE.create«className»()
			}
		}
	'''
}
