package jce.codemanipulation.ecore

import jce.codemanipulation.AbstractCodeManipulator
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.JavaModelException
import jce.properties.EcorificationProperties
import jce.properties.TextProperty
import jce.util.jdt.ASTUtil
import jce.util.PathHelper
import eme.generator.GeneratedEcoreMetamodel
import static extension jce.util.EPackageUtil.*;

/**
 * Code manipulator that correct references to factories in the package classes generated
 * from the Ecore metamodel. Due to the replacement of factories, the new instead of the old
 * ones have to be referenced.
 * 
 * @author Heiko Klare
 */
class PackageImplFactoryCorrector extends AbstractCodeManipulator {
	val extension PathHelper pathHelper;
	val GeneratedEcoreMetamodel metamodel;
	
	new(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
		super(properties.get(TextProperty.ECORE_PACKAGE), properties)
		this.metamodel = metamodel;
		this.pathHelper = new PathHelper(".");
	}

	override protected manipulate(ICompilationUnit unit) throws JavaModelException {
		val unitName = getPackageMemberName(unit);
		val String packageName =
			if (unitName.endsWith(PackageImplFactoryCorrectionVisitor.PACKAGE_IMPL_SUFFIX)) {
				unitName.parent.parent;
			} else if (unitName.endsWith(PackageImplFactoryCorrectionVisitor.PACKAGE_SUFFIX)) {
				unitName.parent;
			} else {
				return;
			}
		val package = metamodel.root.findPackage(packageName);
		// All packages containing classes got new factories that have to be referenced in package classes
		if (!package.classNames.empty) {
			val visitor = new PackageImplFactoryCorrectionVisitor(unit, properties);
			ASTUtil.applyVisitorModifications(unit, visitor, monitor);
			unit.commitWorkingCopy(true, monitor);
			unit.discardWorkingCopy;
			monitor.beginTask("Corrected factory in: " + getPackageMemberName(unit), 0);
		}
	}
	
}