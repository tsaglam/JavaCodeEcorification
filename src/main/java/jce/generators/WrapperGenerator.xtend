package jce.generators

import eme.generator.GeneratedEcoreMetamodel
import jce.properties.EcorificationProperties
import jce.properties.TextProperty
import jce.util.ResourceRefresher
import org.eclipse.core.resources.IProject
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage

import static jce.properties.TextProperty.SOURCE_FOLDER
import static jce.properties.TextProperty.WRAPPER_PACKAGE

/** 
 * Creates and manages wrappers for the classes of the original Java project with is ecorified.
 * @author Timur Saglam
 */
final class WrapperGenerator extends ClassGenerator { // TODO (HIGH) use extensions
	final String sourceFolder
	final String wrapperFolder
	IProject project

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		super(properties)
		sourceFolder = properties.get(SOURCE_FOLDER)
		wrapperFolder = pathUtil.append(sourceFolder, properties.get(WRAPPER_PACKAGE))
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def void buildWrappers(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the wrapper generation...")
		this.project = project
		createFolder(wrapperFolder, project) // build wrapper base folder
		buildWrappers(metamodel.root, "")
		ResourceRefresher.refresh(project, sourceFolder) // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the wrapper creation.
	 * @param ePackage is the current {@link EPackage} to create wrappers for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private void buildWrappers(EPackage ePackage, String path) {
		if(containsEClass(ePackage)) { // avoids empty folders
			createFolder(pathUtil.append(wrapperFolder, path), project)
		}
		for (eClassifier : ePackage.EClassifiers) { // for every classifier
			if(eClassifier instanceof EClass) { // if is EClass
				if(!eClassifier.interface && !isRootContainer(eClassifier, path)) { // if is not interface or root
					createXtendWrapper(eClassifier, path) // create wrapper class
				}
			}
		}
		for (eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildWrappers(eSubpackage, pathUtil.append(path, eSubpackage.name)) // do the same
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
		val wrapper = new WrapperRepresentation(eClass, properties) // build wrapper representation
		val wrapperPath = pathUtil.append(properties.get(WRAPPER_PACKAGE), path) // add wrapper prefix
		createClass(wrapperPath, '''«wrapper.name».xtend''', wrapper.content, project) // create wrapper
	}

	/**
	 * Checks whether a EClass at a given path is the root container element.
	 */
	def private boolean isRootContainer(EClass eClass, String path) {
		return "" === path && eClass.name === properties.get(TextProperty.ROOT_CONTAINER);
	}
}
