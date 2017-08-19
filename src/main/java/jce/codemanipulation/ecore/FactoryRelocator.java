package jce.codemanipulation.ecore;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveCuUpdateCreator;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MetamodelSearcher;
import jce.util.PackageFilter;
import jce.util.PathHelper;

/**
 * Code manipulator that moves the original Ecore factory implementation class into a new subpackage.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction")  // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public class FactoryRelocator extends AbstractCodeManipulator {
    private PathHelper packageUtil;
    private final GeneratedEcoreMetamodel metamodel;

    /**
     * Simple constructor, sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FactoryRelocator(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties);
        this.metamodel = metamodel;
        packageUtil = new PathHelper('.');
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsWith(project, properties.get(TextProperty.ECORE_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreFactory(unit)) {
            System.err.println(unit.getElementName() + " gets moved!"); // TODO
            try {
                IPackageFragment newPackage = null;  // TODO
                relocate(unit, newPackage);
            } catch (CoreException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void relocate(ICompilationUnit unit, IPackageFragment newPackage) throws CoreException {
        CompositeChange composite = new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move);
        MoveCuUpdateCreator creator = new MoveCuUpdateCreator(new ICompilationUnit[] { unit }, newPackage);
        TextChangeManager changeManager = creator.createChangeManager(monitor, new RefactoringStatus());
        composite.merge(new CompositeChange(RefactoringCoreMessages.MoveRefactoring_reorganize_elements, changeManager.getAllChanges()));
        Change change = new MoveCompilationUnitChange(unit, newPackage);
        if (change instanceof CompositeChange) {
            composite.merge(((CompositeChange) change));
        } else {
            composite.add(change);
        }
        composite.perform(monitor);
    }

    private boolean isEcoreFactory(ICompilationUnit unit) throws JavaModelException {
        String fullName = getPackageMemberName(unit); // TODO
        String typeName = packageUtil.cutFirstSegment(fullName);
        String packageName = packageUtil.getLastSegment(packageUtil.cutLastSegments(fullName, 2));
        if (typeName.endsWith(PathHelper.capitalize(packageName) + "FactoryImpl")) { // if has factory name
            return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) == null; // search metamodel counterpart
        }
        return false; // Does not have Ecore implementation name and package
    }
}
