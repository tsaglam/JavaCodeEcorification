package jce.manipulation;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import eme.generator.GeneratedEcoreMetamodel;
import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MetamodelSearcher;
import jce.util.MonitorFactory;
import jce.util.PackageFilter;
import jce.util.PathHelper;

/**
 * Base class for the adaption of problematic import declarations in the Ecore code. A problematic import declaration is
 * any import declaration that references a type from the ecore package which has a counterpart in the Ecore metamodel
 * and the origin code. Unproblematic is any import declaration referencing Ecore package types or Ecore factory types.
 * @author Timur Saglam
 */
public class EcoreImportManipulator extends CodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;
    private final IProgressMonitor monitor;
    private final PathHelper path;

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public EcoreImportManipulator(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties);
        this.metamodel = metamodel;
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
        path = new PathHelper('.'); // package helper
    }

    /**
     * Edits an {@link IImportDeclaration} with the help of an {@link ImportRewrite} instance to refer to the origin
     * code instead to the Ecore code.
     */
    private void edit(IImportDeclaration importDeclaration, ImportRewrite importRewrite) {
        String name = importDeclaration.getElementName();
        if (importRewrite.removeImport(name)) { // remove old import
            String referencedType = importRewrite.addImport(path.cutFirstSegment(name)); // add adapted import
            if (properties.get(BinaryProperty.FULL_LOGGING)) {
                logger.info("Adapted Ecore import " + name + " to " + referencedType);
            }
        } else {
            logger.fatal("Could not remove Ecore import " + name);
        }
    }

    /**
     * Checks whether an {@link ICompilationUnit} is an Ecore package type. Ecore package types are the package
     * interface and implementation class, the factory interface and implementation class, the switch class and the
     * adapter factory of an Ecore package.
     */
    private boolean isEcorePackageType(ICompilationUnit unit) {
        return true; // TODO (HIGH) implement this method.
    }

    /**
     * Checks whether an {@link IImportDeclaration} is problematic. A problematic {@link IImportDeclaration} is any
     * {@link IImportDeclaration} that references a type from the ecore package which has a counterpart in the Ecore
     * metamodel and the origin code. Unproblematic is any {@link IImportDeclaration} referencing Ecore package types or
     * Ecore factory types.
     */
    private boolean isProblematic(IImportDeclaration importDeclaration) {
        if (importDeclaration.isOnDemand()) {
            return false; // EMF imports the Ecore classes directly, not with .*
        }
        return MetamodelSearcher.findEClass(importDeclaration.getElementName(), metamodel.getRoot()) != null;
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsWith(project, properties.get(TextProperty.ECORE_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (!isEcorePackageType(unit)) { // is interface or implementation class of an EClass
            IImportDeclaration[] importDeclarations = unit.getImports();
            ImportRewrite importRewrite = ImportRewrite.create(unit, true);
            for (IImportDeclaration importDeclaration : importDeclarations) {
                if (isProblematic(importDeclaration)) { // edit every problematic import declaration
                    edit(importDeclaration, importRewrite);
                }
            }
            try {
                importRewrite.rewriteImports(monitor); // apply changes.
            } catch (CoreException exception) {
                exception.printStackTrace();
            }
        }
    }
}