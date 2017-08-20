package jce.generators

import eme.generator.GeneratedEcoreMetamodel
import java.io.File
import java.util.LinkedList
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import jce.util.ResourceRefresher
import jce.util.logging.MonitorFactory
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage

import static jce.properties.TextProperty.ECORE_PACKAGE
import static jce.properties.TextProperty.SOURCE_FOLDER

/** 
 * Creates and manages custom EFactories. Every EFactory has an interface and an implementation class.
 * @author Timur Saglam
 */
final class EcoreFactoryGenerator {
	static final Logger logger = LogManager.getLogger(WrapperGenerator.getName)
	final IProgressMonitor monitor
	final PathHelper pathUtil
	final EcorificationProperties properties
	final EFactoryGenerator factoryGenerator
	final EFactoryImplementationGenerator factoryImplementationGenerator
	final boolean buildInterfaces;

	/**
	 * Basic constructor, sets the properties.
	 */
	new(boolean buildInterfaces, EcorificationProperties properties) {
		this.buildInterfaces = buildInterfaces;
		this.properties = properties
		factoryGenerator = new EFactoryGenerator(properties)
		factoryImplementationGenerator = new EFactoryImplementationGenerator(properties)
		monitor = MonitorFactory.createProgressMonitor(logger, properties)
		pathUtil = new PathHelper(File.separatorChar)
	}

	/** 
	 * Builds the wrapper classes.
	 * @param metamodel is the metamodel that got extracted from the original project.
	 * @param directories is the {@link ProjectDirectories} instance for the project.
	 */
	def void buildFactories(GeneratedEcoreMetamodel metamodel, IProject project) {
		logger.info("Starting the factory generation...")
		for (subpackage : metamodel.root.ESubpackages) {
			buildFactories(subpackage, pathUtil.append(properties.get(ECORE_PACKAGE), subpackage.name), project)
		}
		ResourceRefresher.refresh(project, properties.get(SOURCE_FOLDER)) // makes wrappers visible in the Eclipse IDE
	}

	/** 
	 * Recursive method for the factory creation.
	 * @param ePackage is the current {@link EPackage} to create factories for.
	 * @param path is the current file path of the {@link EPackage}. Should be initially an empty string.
	 */
	def private void buildFactories(EPackage ePackage, String path, IProject project) {
		val types = new LinkedList<String>()
		for (eClassifier : ePackage.EClassifiers) { // for every classifier
			if (eClassifier instanceof EClass) { // if is EClass
				if (!eClassifier.interface && !eClassifier.abstract) { // if is not interface
					types.add(eClassifier.name) // store name
				}
			}
		}
		if (!types.empty) {
			if (buildInterfaces) {
				factoryGenerator.create(path, types, project) // create factory interface and implementation
			}
			factoryImplementationGenerator.create(pathUtil.append(path, "impl"), types, project)
			monitor.subTask("Created factory interface and implementation for EPackage " + ePackage.name)
		}
		for (eSubpackage : ePackage.ESubpackages) { // for every subpackage
			buildFactories(eSubpackage, pathUtil.append(path, eSubpackage.name), project) // do the same
		}
	}
}
