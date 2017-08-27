package jce.codemanipulation.ecore;

import static jce.properties.TextProperty.FACTORY_SUFFIX;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MetamodelSearcher;
import jce.util.PackageFilter;
import jce.util.PathHelper;
import jce.util.RefactoringUtil;

/**
 * Code manipulator that moves the original Ecore factory implementation class into a new subpackage.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction") // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public class FactoryRenamer extends AbstractCodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;

    /**
     * Simple constructor, sets the metamodel and the properties.
     * @param metamodel is the extracted Ecore metamodel.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FactoryRenamer(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties);
        this.metamodel = metamodel;
    }

    /**
     * Checks whether a {@link ICompilationUnit} is an Ecore factory implementation class.
     */
    private boolean isEcoreFactory(ICompilationUnit unit) throws JavaModelException { // TODO (HIGH) allow interfaces
        String fullName = getPackageMemberName(unit); // get name of the type
        String packageName = packageHelper.getLastSegment(packageHelper.cutLastSegments(fullName, 2));
        if (fullName.endsWith(PathHelper.capitalize(packageName) + "FactoryImpl")) { // if has factory name
            String modelName = packageHelper.cutFirstSegment(fullName); // without ecore package
            return MetamodelSearcher.findEClass(modelName, metamodel.getRoot()) == null; // search metamodel counterpart
        }
        return false; // Does not have Ecore implementation name and package
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsWith(project, properties.get(TextProperty.ECORE_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreFactory(unit)) {
            String name = unit.getElementName();
            String newName = packageHelper.cutLastSegment(name) + properties.get(FACTORY_SUFFIX); // new name of class
            newName = packageHelper.append(newName, packageHelper.getLastSegment(name)); // add file extension
            try {
                RenameCompilationUnitProcessor processor = new RenameCompilationUnitProcessor(unit);
                if (processor.checkNewElementName(newName).isOK()) { // if new name is okay
                    processor.setNewElementName(newName); // set new name
                }
                RenameRefactoring refactoring = new RenameRefactoring(processor); // create refactoring
                RefactoringUtil.applyRefactoring(refactoring, monitor); // apply refactoring
                monitor.beginTask("Renamed factory: " + name, 0);
            } catch (CoreException exception) {
                logger.fatal("Renaming " + unit.getElementName() + " failed!", exception);
            }
        }
    }
}
