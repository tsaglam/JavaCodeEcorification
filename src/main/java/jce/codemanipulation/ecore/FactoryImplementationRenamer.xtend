package jce.codemanipulation.ecore

import eme.generator.GeneratedEcoreMetamodel
import jce.properties.EcorificationProperties
import jce.util.PathHelper
import org.eclipse.jdt.core.JavaModelException

import static extension jce.util.PathHelper.capitalize
import jce.util.jdt.ASTUtil
import org.eclipse.jdt.core.ICompilationUnit

/**
 * Code manipulator that renames the original factory implementation classes.
 * @author Timur Saglam
 * @author Heiko Klare added init method manipulation
 */
class FactoryImplementationRenamer extends AbstractFactoryRenamer {
	extension PathHelper nameUtil = super.nameUtil

	/**
	 * Simple constructor, sets the metamodel and the properties.
	 * @param metamodel is the extracted Ecore metamodel.
	 * @param properties are the {@link EcorificationProperties}.
	 */
	new(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
		super(metamodel, properties)
	}

	/**
	 * Checks whether a Java type has the name of an Ecore factory implementation class.
	 * @param fullName is the fully qualified name of the java type.
	 * @return true if it has a factory name.
	 * @throws JavaModelException if there is a problem with the JDT API.
	 */
	override protected hasFactoryName(String fullName) throws JavaModelException {
		val packageName = fullName.cutLastSegments(2).lastSegment
		if(fullName.endsWith(packageName.capitalize + "FactoryImpl")) { // if has factory name
			return !fullName.isInMetamodel // check if not in metamodel
		}
		return false; // Does not have Ecore implementation name and package
	}

	/**
	 * Removes the namespace URI-based factory determination in originally generated factories,
	 * because they are only used internally and not globally registered.
	 * Afterwards applies the general manipulation logic of
	 * {@link AbstractFactoryRenamer#manipulate(ICompilationUnit) AbstractFactoryRenamer}.
	 * @param unit the {@link ICompilationUnit} to manipulate
	 */
	override manipulate(ICompilationUnit unit) {
		if (isRelevantEcoreFactoryClassifier(unit)) {
			ASTUtil.applyVisitorModifications(unit, new FactoryInitMethodCorrectionVisitor(), monitor);
			unit.commitWorkingCopy(true, monitor);
			unit.discardWorkingCopy();
		}
		super.manipulate(unit);
	}

}
