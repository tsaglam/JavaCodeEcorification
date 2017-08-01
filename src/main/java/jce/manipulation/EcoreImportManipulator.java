package jce.manipulation;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

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
 * any import declaration that references a type from the Ecore package which has a counterpart in the Ecore metamodel
 * and the origin code. Unproblematic is any import declaration referencing Ecore package types or Ecore factory types.
 * This class basically changes all problematic imports in all Ecore implementation classes and adds the changed imports
 * to the correlating Ecore interfaces while retaining the correct super interfaces of the implementation classes.
 * @author Timur Saglam
 */
public class EcoreImportManipulator extends CodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;
    private final IProgressMonitor monitor;
    private final PathHelper path;
    private IJavaProject project;

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

    @Override
    public void manipulate(IProject project) {
        this.project = JavaCore.create(project); // makes the project instance available in this class.
        super.manipulate(project);
    }

    /**
     * Applies all recorded changes of an {@link ImportRewrite} to an {@link ICompilationUnit}.
     */
    private void applyChanges(ICompilationUnit unit, ImportRewrite importRewrite) {
        if (importRewrite.hasRecordedChanges()) { // apply changes if existing
            logChange(unit, importRewrite); // log the changed imports
            try {
                TextEdit edits = importRewrite.rewriteImports(monitor); // create text edit
                applyEdits(edits, unit); // apply text edit to compilation unit.
            } catch (MalformedTreeException exception) {
                logger.fatal(exception);
            } catch (CoreException exception) {
                logger.fatal(exception);
            }
        }
    }

    /**
     * Edits an {@link IImportDeclaration} with the help of an {@link ImportRewrite} instance to refer to the origin
     * code instead to the Ecore code.
     */
    private void edit(IImportDeclaration importDeclaration, ImportRewrite implementationRewrite, ImportRewrite interfaceRewrite) {
        String name = importDeclaration.getElementName();
        if (implementationRewrite.removeImport(name)) { // remove old import
            name = path.cutFirstSegment(name); // generate new import string
            implementationRewrite.addImport(name); // add to implementation class
            interfaceRewrite.addImport(name); // add to Ecore interface
        } else {
            logger.fatal("Could not remove Ecore import " + name);
        }
    }

    /**
     * Finds the Ecore interface of an {@link ICompilationUnit} which is an Ecor eimplementation class.
     */
    private ICompilationUnit findEcoreInterface(ICompilationUnit unit) throws JavaModelException {
        String interfaceName = getInterfaceName(getPackageMemberName(unit));
        IType iType = project.findType(interfaceName);
        return iType.getCompilationUnit();
    }

    /**
     * Returns the name of the Ecore interface of an Ecore implementation class name. E.g. returns "model.Main" when
     * given "model.impl.MainImpl".
     */
    private String getInterfaceName(String typeName) {
        String interfaceName = path.append(path.cutLastSegments(typeName, 2), path.getLastSegment(typeName));
        return interfaceName.substring(0, interfaceName.length() - 4); // remove "Impl" suffix
    }

    /**
     * Returns the name of the package member type of a compilation unit. E.g. "model.Main" from "Main.java"
     */
    private String getPackageMemberName(ICompilationUnit unit) throws JavaModelException {
        CompilationUnit parsedUnit = parse(unit);
        TypeNameResolver visitor = new TypeNameResolver();
        parsedUnit.accept(visitor);
        return visitor.getTypeName();
    }

    /**
     * Checks whether an {@link ICompilationUnit} is an Ecore implementation class. Ecore implementation classes are the
     * classes that implement the Ecore interfaces. The Ecore implementation classes are the types whose imports should
     * be edited. This method first checks the compilation unit on correct naming and then on the existence of a
     * counterpart in the Ecore metamodel
     */
    private boolean isEcoreImplementation(ICompilationUnit unit) throws JavaModelException {
        String typeName = path.cutFirstSegment(getPackageMemberName(unit));
        if (isEcoreImplementationName(typeName)) { // if has Ecore implementation name and package
            typeName = getInterfaceName(typeName); // get name of Ecore interface and EClass
            return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) != null; // search metamodel counterpart
        }
        return false; // Does not have Ecore implementation name and package
    }

    /**
     * Checks whether a fully qualified type name identifies a type which could be part of an Ecore implementation
     * package. That means the last package of the type name is called impl and the type name end with the suffix Impl.
     */
    private boolean isEcoreImplementationName(String typeName) {
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

    /**
     * Logs the changed import if full logging is enabled in the {@link EcorificationProperties}.
     */
    private void logChange(ICompilationUnit unit, ImportRewrite rewrite) {
        if (properties.get(BinaryProperty.FULL_LOGGING)) {
            logger.info(unit.getElementName() + ": removed " + Arrays.toString(rewrite.getRemovedImports()) + ", added "
                    + Arrays.toString(rewrite.getAddedImports()));
        }
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsWith(project, properties.get(TextProperty.ECORE_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreImplementation(unit)) { // is interface or implementation class of an EClass
            applyVisitorModifications(unit, new InterfaceRetentionVisitor(unit.getParent().getElementName()));
            IImportDeclaration[] importDeclarations = unit.getImports();
            ICompilationUnit interfaceUnit = findEcoreInterface(unit);
            ImportRewrite implementationRewrite = ImportRewrite.create(unit, true);
            ImportRewrite interfaceRewrite = ImportRewrite.create(interfaceUnit, true);
            for (IImportDeclaration importDeclaration : importDeclarations) {
                if (isProblematic(importDeclaration)) { // edit every problematic import declaration
                    edit(importDeclaration, implementationRewrite, interfaceRewrite);
                }
            }
            applyChanges(unit, implementationRewrite);
            applyChanges(interfaceUnit, interfaceRewrite);
        }
    }

    /**
     * AST visitor that resolves the name of the package member type of the compilation unit.
     * @author Timur Saglam
     */
    private class TypeNameResolver extends ASTVisitor {
        private String typeName;
    
        /**
         * Getter for the resolved type name.
         * @return the fully qualified name of the resolved type binding of the type declaration.
         */
        public String getTypeName() {
            return typeName;
        }
    
        @Override
        public boolean visit(TypeDeclaration node) {
            if (node.isPackageMemberTypeDeclaration()) {
                typeName = node.getName().resolveTypeBinding().getQualifiedName(); // fully qualified name of class
            }
            return super.visit(node);
        }
    }
}