package jce.codemanipulation.ecore;

import static jce.properties.TextProperty.FACTORY_PACKAGE;
import static jce.properties.TextProperty.SOURCE_FOLDER;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import jce.util.EcoreUtil;
import jce.util.PathHelper;

/**
 * Code manipulator that moves the original Ecore factory implementation class into a new subpackage.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction")  // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public class FactoryRelocator extends AbstractCodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;

    /**
     * Simple constructor, sets the metamodel and the properties.
     * @param metamodel is the extracted Ecore metamodel.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FactoryRelocator(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties.get(TextProperty.ECORE_PACKAGE), properties);
        this.metamodel = metamodel;
    }

    /**
     * Creates a new factory package in the original package of the {@link ICompilationUnit}. Returns the
     * {@link IPackageFragment}.
     */
    private IPackageFragment createFactoryPackage(ICompilationUnit unit) throws JavaModelException {
        IJavaProject javaProject = unit.getJavaProject();
        // get the source folder root:
        IFolder folder = javaProject.getProject().getFolder(properties.get(SOURCE_FOLDER));
        IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
        // build the new package:
        String currentPackage = unit.getParent().getElementName();
        String newPackageName = nameUtil.append(currentPackage, properties.get(FACTORY_PACKAGE));
        return root.createPackageFragment(newPackageName, false, monitor);
    }

    /**
     * Checks whether a {@link ICompilationUnit} is an Ecore factory implementation class.
     */
    private boolean isEcoreFactory(ICompilationUnit unit) throws JavaModelException {
        String fullName = getPackageMemberName(unit); // get name of the type
        String packageName = nameUtil.getLastSegment(nameUtil.cutLastSegments(fullName, 2));
        if (fullName.endsWith(PathHelper.capitalize(packageName) + "FactoryImpl")) { // if has factory name
            String modelName = nameUtil.cutFirstSegment(fullName); // without ecore package
            return EcoreUtil.findEClass(modelName, metamodel.getRoot()) == null; // search metamodel counterpart
        }
        return false; // Does not have Ecore implementation name and package
    }

    /**
     * Moves an {@link ICompilationUnit} into a new {@link IPackageFragment} while updating all references.
     */
    private void relocate(ICompilationUnit unit, IPackageFragment newPackage) throws CoreException {
        CompositeChange composite = new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move);
        MoveCuUpdateCreator creator = new MoveCuUpdateCreator(new ICompilationUnit[] { unit }, newPackage);
        TextChangeManager changeManager = creator.createChangeManager(monitor, new RefactoringStatus());
        composite.merge(new CompositeChange(RefactoringCoreMessages.MoveRefactoring_reorganize_elements, changeManager.getAllChanges()));
        Change change = new MoveCompilationUnitChange(unit, newPackage); // This is all taken from Eclipse code.
        if (change instanceof CompositeChange) {
            composite.merge(((CompositeChange) change));
        } else {
            composite.add(change);
        }
        composite.perform(monitor);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreFactory(unit)) {
            IPackageFragment factoryPackage = createFactoryPackage(unit);
            try { // move factory to new package:
                relocate(unit, factoryPackage);
            } catch (CoreException exception) {
                exception.printStackTrace();
            }
        }
    }
}
