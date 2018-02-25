package jce.codemanipulation.ecore;

import static jce.properties.TextProperty.ECORE_PACKAGE;
import static jce.properties.TextProperty.FACTORY_SUFFIX;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MetamodelSearcher;
import jce.util.PathHelper;
import jce.util.jdt.RefactoringUtil;

/**
 * Abstract code manipulator that renames the original factories.
 * @author Timur Saglam
 */
@SuppressWarnings("restriction") // TODO (LOW) This class uses LTK classes & methods that are not marked as API
public abstract class AbstractFactoryRenamer extends AbstractCodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;
    private final String rootFactory;
    private final String rootFactoryImplementation;

    /**
     * Simple constructor, sets the metamodel and the properties.
     * @param metamodel is the extracted Ecore metamodel.
     * @param properties are the {@link EcorificationProperties}.
     */
    public AbstractFactoryRenamer(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties.get(TextProperty.ECORE_PACKAGE), properties);
        this.metamodel = metamodel;
        String ecorePackage = properties.get(ECORE_PACKAGE);
        rootFactory = nameUtil.append(ecorePackage, PathHelper.capitalize(ecorePackage) + "Factory");
        rootFactoryImplementation = nameUtil.append(ecorePackage, "impl", PathHelper.capitalize(ecorePackage) + "FactoryImpl");
    }

    /**
     * Checks whether an {@link ICompilationUnit} is a Ecore factory, which means it is either an Ecore factory
     * interface or and Ecore factory implementation class.
     */
    private boolean isEcoreFactory(ICompilationUnit unit) throws JavaModelException {
        String fullName = getPackageMemberName(unit); // get name
        return hasFactoryName(fullName);
    }

    /**
     * Checks whether an {@link ICompilationUnit} has the name and package of the factory of the root container package.
     */
    private boolean isRootFactory(ICompilationUnit unit) throws JavaModelException {
        String name = getPackageMemberName(unit);
        return rootFactory.equals(name) || rootFactoryImplementation.equals(name);
    }

    /**
     * Actually renames the {@link ICompilationUnit} with the given name.
     */
    private void rename(ICompilationUnit unit, String newName) throws JavaModelException {
        try {
            RenameCompilationUnitProcessor processor = new RenameCompilationUnitProcessor(unit); // create processor
            setNewName(unit, newName, processor); // check and set new name
            RenameRefactoring refactoring = new RenameRefactoring(processor); // create refactoring from processor
            RefactoringUtil.applyRefactoring(refactoring, monitor, logger);
        } catch (CoreException exception) {
            logger.fatal("Renaming " + getPackageMemberName(unit) + " failed!", exception);
        }
    }

    /**
     * Checks and sets a new name with a {@link RenameCompilationUnitProcessor}. This makes sure the name is acceptable.
     */
    private void setNewName(ICompilationUnit unit, String newName, RenameCompilationUnitProcessor processor) throws CoreException {
        if (processor.checkNewElementName(newName).isOK()) { // if new name is okay
            processor.setNewElementName(newName); // set new name
        } else {
            logger.error("Could not rename " + unit.getElementName() + " to " + newName);
        }
    }

    /**
     * Checks whether a Java type has the name of an Ecore factory.
     * @param fullName is the fully qualified name of the java type.
     * @return true if it has a factory name.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    protected abstract boolean hasFactoryName(String fullName) throws JavaModelException;

    /**
     * Checks if a type from the model code is in the metamodel.
     * @param fullName is the fully qualified name of the type.
     * @return true if it is an {@link EClass} in the metamodel.
     */
    protected final boolean isInMetamodel(String fullName) {
        String modelName = nameUtil.cutFirstSegment(fullName); // remove leading ecore package
        return MetamodelSearcher.findEClass(modelName, metamodel.getRoot()) != null; // search metamodel counterpart
    }

    @Override
    protected final void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreFactory(unit) && !isRootFactory(unit)) {
            String name = unit.getElementName();
            String newName = nameUtil.cutLastSegment(name) + properties.get(FACTORY_SUFFIX); // new name of class
            newName = nameUtil.append(newName, nameUtil.getLastSegment(name)); // add file extension
            rename(unit, newName);
            monitor.beginTask("Renamed factory: " + getPackageMemberName(unit), 0);
        }
    }
}
