package jce.generators

import eme.generator.GeneratedEcoreMetamodel
import jce.properties.EcorificationProperties
import jce.util.ResourceRefresher
import org.eclipse.core.resources.IProject
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore

import static jce.properties.TextProperty.ROOT_CONTAINER
import static jce.properties.TextProperty.SOURCE_FOLDER
import static jce.properties.TextProperty.WRAPPER_PACKAGE

/** 
 * Creates and manages wrappers for the classes of the original Java project with is ecorified.
 * @author Timur Saglam
 */
final class WrapperGenerator extends ClassGenerator {
	IJavaProject javaProject
	GeneratedEcoreMetamodel metamodel

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		super(properties)
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def void buildWrappers(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the wrapper generation...")
		this.javaProject = JavaCore.create(project)
		this.metamodel = metamodel
		createFolder(wrapperFolder, project) // build wrapper base folder
		buildWrappers(metamodel.root, "")
		ResourceRefresher.refresh(project, SOURCE_FOLDER.get) // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the wrapper creation.
	 * @param ePackage is the current {@link EPackage} to create wrappers for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private void buildWrappers(EPackage ePackage, String path) {
		if(containsEClass(ePackage)) { // avoids empty folders
			createFolder(append(wrapperFolder, path), javaProject.project)
		}
		for (eClassifier : ePackage.EClassifiers) { // for every classifier
			if(eClassifier instanceof EClass) { // if is EClass
				if(!eClassifier.interface && !isRootContainer(eClassifier, path)) { // if is not interface or root
					createXtendWrapper(eClassifier, path) // create wrapper class
				}
			}
		}
		for (eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildWrappers(eSubpackage, append(path, eSubpackage.name)) // do the same
		}
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
	 * Creates a Xtend Wrapper in a package path with a specific name. 
	 */
	def private void createXtendWrapper(EClass eClass, String path) {
		val wrapper = new WrapperRepresentation(eClass, javaProject, metamodel.intermediateModel, properties) // build wrapper representation
		val wrapperPath = append(WRAPPER_PACKAGE.get, path) // add wrapper prefix
		createClass(wrapperPath, '''«wrapper.name».xtend''', wrapper.content, javaProject.project) // create wrapper
	}

	/**
	 * Checks whether a EClass at a given path is the root container element.
	 */
	def private boolean isRootContainer(EClass eClass, String path) {
		return "" === path && eClass.name === ROOT_CONTAINER.get
	}

	/**
	 * Generates the wrapper folder path by appending the source folder path and the wrapper package name.
	 */
	def private String wrapperFolder() {
		append(SOURCE_FOLDER.get, WRAPPER_PACKAGE.get)
	}
}
