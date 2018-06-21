package jce.generators

import eme.generator.GeneratedEcoreMetamodel
import java.io.File
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import jce.util.ResourceRefresher
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IProject
import org.eclipse.emf.ecore.EPackage

import static jce.properties.TextProperty.ECORE_PACKAGE
import static jce.properties.TextProperty.SOURCE_FOLDER

import static extension jce.util.EPackageUtil.*;

/** 
 * Creates and manages custom EFactories. Every EFactory has an interface and an implementation class.
 * @author Timur Saglam
 */
final class EcoreFactoryGenerator {
	extension final PathHelper pathUtil
	extension final EcorificationProperties properties
	
	static final Logger logger = LogManager.getLogger(EcoreFactoryGenerator.getName)
	final EFactoryGenerator factoryGenerator
	final EFactoryImplementationGenerator factoryImplementationGenerator

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		this.properties = properties
		factoryGenerator = new EFactoryGenerator(properties)
		factoryImplementationGenerator = new EFactoryImplementationGenerator(properties)
		pathUtil = new PathHelper(File.separatorChar)
	}

	/** 
	 * Builds the Ecore factories.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def void buildFactories(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the factory generation...")
		for (subpackage : metamodel.root.ESubpackages) { // build factories for every supackage
			buildFactories(subpackage, append(ECORE_PACKAGE.get, subpackage.name), project)
		}
		ResourceRefresher.refresh(project, SOURCE_FOLDER.get) // makes factories visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the factory creation.
	 * @param ePackage is the current {@link EPackage} to create factories for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private void buildFactories(EPackage ePackage, String path, IProject project) {
		val classes = getClassNames(ePackage)
		if (!classes.empty) {
			factoryGenerator.create(path, classes, project) // create interface
			factoryImplementationGenerator.create(append(path, "impl"), classes, project) // create implementation
		}
		for (eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildFactories(eSubpackage, append(path, eSubpackage.name), project) // do the same
		}
	}

}
