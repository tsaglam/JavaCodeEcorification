package jce.manipulation;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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
     * @param metamodel is the extracted Ecore metamodel. It is needed to decide which imports to manipulate.
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
    private boolean isEcorePackageType(ICompilationUnit unit) throws JavaModelException {
        CompilationUnit parsedUnit = parse(unit);
        TypeNameResolver visitor = new TypeNameResolver();
        parsedUnit.accept(visitor);
        String typeName = path.cutFirstSegment(visitor.getTypeName());
        if (MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) == null) {
            if (isImplementationClass(typeName)) {
                typeName = path.append(path.cutLastSegments(typeName, 2), path.getLastSegment(typeName));
                typeName = typeName.substring(0, typeName.length() - 4);
                return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) == null;
            }
        } else { // TODO (HIGH) comment & optimize
            return false;
        }
        return true;
    }

    /**
     * Checks whether a fully qualified type name identifies a type which could be part of an Ecore implementation
     * package. That means the last package of the type name is called impl.
     */
    private boolean isImplementationClass(String typeName) {
        System.err.println("   is impl? " + typeName); // TODO
        return path.getLastSegment(path.cutLastSegment(typeName)).equals("impl") && typeName.endsWith("Impl");
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
        String typeName = path.cutFirstSegment(importDeclaration.getElementName());
        return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) != null;
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
                    System.err.println("Edit import " + importDeclaration.getElementName() + " in file " + unit.getElementName()); // TODO
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

    /**
     * AST visitor that resolves the name of the package member type of the compilation unit.
     * @author Timur Saglam
     */
    private class TypeNameResolver extends ASTVisitor {
        private String typeName;

        @Override
        public boolean visit(TypeDeclaration node) {
            if (node.isPackageMemberTypeDeclaration()) {
                typeName = node.getName().resolveTypeBinding().getQualifiedName(); // fully qualified name of class
            }
            return super.visit(node);
        }

        /**
         * Getter for the resolved type name.
         * @return the fully qualified name of the resolved type binding of the type declaration.
         */
        public String getTypeName() {
            return typeName;
        }
    }
}