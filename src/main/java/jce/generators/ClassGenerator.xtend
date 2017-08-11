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
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor

import static jce.properties.TextProperty.SOURCE_FOLDER

class ClassGenerator {
	static final Logger logger = LogManager.getLogger(ClassGenerator.getName)
	final protected PathHelper packageUtil
	final protected PathHelper pathUtil
	final protected IProgressMonitor monitor
	final protected EcorificationProperties properties

	/**
	 * Basic constructor, sets the properties.
	 */
	new(EcorificationProperties properties) {
		this.properties = properties
		monitor = MonitorFactory.createProgressMonitor(logger, properties)
		packageUtil = new PathHelper(Character.valueOf('.').charValue)
		pathUtil = new PathHelper(File.separatorChar)
	}

	/**
	 * Creates an IFile from a project relative path, a file name and creates the file content.
	 */
	def public void createClass(String path, String name, String content, IProject project) {
		var folder = project.getFolder(pathUtil.append(properties.get(SOURCE_FOLDER), path))
		var file = folder.getFile(name)
		if (!file.exists) {
			val source = new ByteArrayInputStream(content.bytes)
			file.create(source, IResource.NONE, monitor)
			file.touch(monitor)
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
}
