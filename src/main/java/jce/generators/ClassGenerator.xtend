package jce.generators

import java.io.ByteArrayInputStream
import java.io.File
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import jce.util.logging.MonitorFactory
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IProgressMonitor

import static jce.properties.TextProperty.SOURCE_FOLDER

class ClassGenerator {
	extension protected final PathHelper pathUtil
	extension protected final EcorificationProperties properties
	
	protected static final Logger logger = LogManager.getLogger(ClassGenerator.name)
	protected final IProgressMonitor monitor

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		this.properties = properties
		monitor = MonitorFactory.createProgressMonitor(logger, properties)
		pathUtil = new PathHelper(File.separatorChar)
	}

	/**
	 * Creates an IFile from a project relative path, a file name and creates the file content.
	 */
	def public void createClass(String path, String name, String content, IProject project) {
		var folder = project.getFolder(append(SOURCE_FOLDER.get, path))
		var file = folder.getFile(name)
		if (file.exists) {
			logger.error("File " + file.name + " already exists!")
		} else {
			val source = new ByteArrayInputStream(content.bytes)
			file.create(source, true, monitor)
		}
	}

	/**
	 * Creates an {@link IFolder} in the project with a project relative path.
	 */
	def public void createFolder(String path, IProject project) {
		var IFolder folder = project.getFolder(path)
		if (!folder.exists) {
			folder.create(false, true, monitor)
		}
	}
	
	/**
	 * Generates a blank line in a template.
	 */
	def protected String getBlankLine() '''
		
	'''
}
