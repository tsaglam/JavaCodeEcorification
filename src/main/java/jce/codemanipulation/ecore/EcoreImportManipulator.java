package jce.codemanipulation.ecore;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.EcoreUtil;
import jce.util.jdt.ASTUtil;

/**
 * Base class for the adaption of problematic import declarations in the Ecore code. A problematic import declaration is
 * any import declaration that references a type from the Ecore package which has a counterpart in the Ecore metamodel
 * and the origin code. Unproblematic is any import declaration referencing Ecore package types or Ecore factory types.
 * This class basically changes all problematic imports in all Ecore implementation classes and adds the changed imports
 * to the correlating Ecore interfaces while retaining the correct super interfaces of the implementation classes. The
 * changed imports refer to the types of the origin code instead of the Ecore code.
 * @author Timur Saglam
 */
public class EcoreImportManipulator extends AbstractCodeManipulator {
    private final GeneratedEcoreMetamodel metamodel;
    private IJavaProject project;

    /**
     * Simple constructor that sets the properties.
     * @param metamodel is the extracted Ecore metamodel. It is needed to decide which imports to manipulate.
     * @param properties are the {@link EcorificationProperties}.
     */
    public EcoreImportManipulator(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties.get(TextProperty.ECORE_PACKAGE), properties);
        this.metamodel = metamodel;
    }

    @Override
    public void manipulate(IProject project) {
        this.project = JavaCore.create(project); // makes the project instance available in this class.
        super.manipulate(project);
    }

    /**
     * Makes all type references to the type with the given name in the given {@link ICompilationUnit} explicit using
     * qualified names.
     * @param unit is the {@link ICompilationUnit} to manipulate.
     * @param typeName is the name of the type to make references to explicit.
     * @throws JavaModelException if there are problems with the Java model.
     */
    private void addQualifiedNamesToTypeReferences(ICompilationUnit unit, String typeName) throws JavaModelException {
        ASTVisitor visitor = new TypeManipulationVisitor(typeName);
        ASTUtil.applyVisitorModifications(unit, visitor, monitor);
    }

    /**
     * Retains the super interface declarations and type parameter bounds of a compilation unit with a specific set of
     * imports.
     */
    private void applyRetentionVisitor(ICompilationUnit unit, IImportDeclaration[] imports) throws JavaModelException {
        ASTVisitor visitor = new TypeRetentionVisitor(unit, imports);
        ASTUtil.applyVisitorModifications(unit, visitor, monitor);
    }

    /**
     * Finds the Ecore implementation class of an {@link ICompilationUnit} which is an Ecore interface.
     */
    private ICompilationUnit findEcoreImplementation(ICompilationUnit unit) throws JavaModelException {
        String implementationName = getImplementationName(getPackageMemberName(unit));
        IType iType = project.findType(implementationName);
        if (iType == null) {
           return unit; // return original, better than nothing TODO (HIGH) make this more elegant
        }
        return iType.getCompilationUnit();
    }

    /**
     * Finds the Ecore interface of an {@link ICompilationUnit} which is an Ecore implementation class.
     */
    private ICompilationUnit findEcoreInterface(ICompilationUnit unit) throws JavaModelException {
        String interfaceName = getInterfaceName(getPackageMemberName(unit));
        IType iType = project.findType(interfaceName);
        return iType.getCompilationUnit();
    }

    /**
     * Checks for cases where the Ecore interface uses instances of itself. In those cases the import manipulation
     * cannot change the type of the instances, because the origin code type import would clash with the Ecore interface
     * itself. Therefore the correlating parameters are retyped manually.
     */
    private void fixSelfImports(ICompilationUnit ecoreInterface, IImportDeclaration importDeclaration) throws JavaModelException {
        String interfaceName = getPackageMemberName(ecoreInterface);
        if (importDeclaration.getElementName().equals(interfaceName)) {
            String typeName = nameUtil.cutFirstSegment(importDeclaration.getElementName());
            addQualifiedNamesToTypeReferences(ecoreInterface, typeName);
        }
    }

    /**
     * Returns the name of the Ecore implementation class of an Ecore interface name. E.g. returns "model.impl.MainImpl"
     * when given "model.Main".
     */
    private String getImplementationName(String typeName) {
        return nameUtil.append(nameUtil.getParent(typeName), "impl", nameUtil.getLastSegment(typeName) + "Impl");
    }

    /**
     * Returns the name of the Ecore interface of an Ecore implementation class name. E.g. returns "model.Main" when
     * given "model.impl.MainImpl".
     */
    private String getInterfaceName(String typeName) {
        String interfaceName = nameUtil.append(nameUtil.cutLastSegments(typeName, 2), nameUtil.getLastSegment(typeName));
        return interfaceName.substring(0, interfaceName.length() - 4); // remove "Impl" suffix
    }

    /**
     * Checks whether an {@link ICompilationUnit} is an Ecore implementation class. Ecore implementation classes are the
     * classes that implement the Ecore interfaces. The Ecore implementation classes are the types whose imports should
     * be edited. This method first checks the compilation unit on correct naming and then on the existence of a
     * counterpart in the Ecore metamodel
     */
    private boolean isEcoreImplementation(ICompilationUnit unit) throws JavaModelException {
        String typeName = nameUtil.cutFirstSegment(getPackageMemberName(unit));
        if (isEcoreImplementationName(typeName)) { // if has Ecore implementation name and package
            typeName = getInterfaceName(typeName); // get name of Ecore interface and EClass
            return EcoreUtil.findEClass(typeName, metamodel.getRoot()) != null; // search metamodel counterpart
        }
        return false; // Does not have Ecore implementation name and package
    }

    /**
     * Checks whether a fully qualified type name identifies a type which could be part of an Ecore implementation
     * package. That means the last package of the type name is called impl and the type name end with the suffix Impl.
     */
    private boolean isEcoreImplementationName(String typeName) {
        return nameUtil.getLastSegment(nameUtil.cutLastSegment(typeName)).equals("impl") && typeName.endsWith("Impl");
    }

    /**
     * Checks and returns whether an {@link ICompilationUnit} is the representation of an Ecore interface.
     */
    private boolean isEcoreInterface(ICompilationUnit unit) throws JavaModelException {
        String typeName = nameUtil.cutFirstSegment(getPackageMemberName(unit));
        EClass potentialEClass = EcoreUtil.findEClass(typeName, metamodel.getRoot());
        return potentialEClass != null && potentialEClass.isInterface();
    }

    /**
     * Checks and returns whether an {@link ICompilationUnit} is the representation of an interface of an Ecore
     * implementation class.
     */
    private boolean isInterfaceOfEcoreClass(ICompilationUnit unit) throws JavaModelException {
        String typeName = nameUtil.cutFirstSegment(getPackageMemberName(unit));
        EClass potentialEClass = EcoreUtil.findEClass(typeName, metamodel.getRoot());
        // Ensure that the class is not the representation of an Ecore interface but only an interface of an
        // implementation class
        return potentialEClass != null && !potentialEClass.isInterface();
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
        String typeName = nameUtil.cutFirstSegment(importDeclaration.getElementName());
        return EcoreUtil.findEClass(typeName, metamodel.getRoot()) != null;
    }

    /**
     * Changes the imports of a compilation unit and its Ecore interface if it is an Ecore implementation class. The
     * super interface declarations of the classes are retained, while the import declarations of the Ecore type are
     * changed to the relating types of the origin code.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    private void manipulateEcoreClass(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreImplementation(unit)) { // if is ecore implementation class of an EClass
            ICompilationUnit ecoreInterface = findEcoreInterface(unit); // get the correlating ecore interface
            retainTypes(unit, ecoreInterface); // retain the super interfaces of both
            rewriteImports(unit, ecoreInterface);
        }
    }

    /**
     * Changes the imports in the same package of a compilation unit if it is an Ecore interface. The super interface
     * declarations of the classes are retained, while the import declarations of the Ecore type are changed to the
     * relating types of the origin code, which are not represented as imports, as the Ecore types of the interfaces
     * reside in the same package.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    private void manipulateEcoreInterface(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreInterface(unit)) {
            ICompilationUnit ecoreImplementation = findEcoreImplementation(unit);
            retainTypes(ecoreImplementation, unit);
            ICompilationUnit[] unitsInSamePackage = ((IPackageFragment) unit.getParent()).getCompilationUnits();
            // Add explicit import for types in same package
            for (ICompilationUnit samePackageUnit : unitsInSamePackage) {
                if (isEcoreInterface(samePackageUnit) || isInterfaceOfEcoreClass(samePackageUnit)) {
                    String samePackageUnitQualifiedName = nameUtil.cutFirstSegment(getPackageMemberName(samePackageUnit));
                    ImportRewrite explicitSamePackageImportRewrite = ImportRewrite.create(unit, true);
                    explicitSamePackageImportRewrite.addImport(samePackageUnitQualifiedName);
                    ASTUtil.applyImportRewrite(unit, explicitSamePackageImportRewrite, monitor);
                    // Fix imports of type with same name
                    if (getPackageMemberName(samePackageUnit).equals(getPackageMemberName(unit))) {
                        addQualifiedNamesToTypeReferences(unit, samePackageUnitQualifiedName);
                    }
                }
            }
            // Change imports in same package to those of original classes
            rewriteImports(unit, unit);
        }
    }

    /**
     * Retains the super interface declarations and type parameter bounds of the Ecore interface and its implementation.
     */
    private void retainTypes(ICompilationUnit ecoreImplementation, ICompilationUnit ecoreInterface) throws JavaModelException {
        // always use the implementation imports to resolve classes in the same package with the interface:
        applyRetentionVisitor(ecoreImplementation, ecoreImplementation.getImports());
        applyRetentionVisitor(ecoreInterface, ecoreImplementation.getImports());
    }

    /**
     * Edits an {@link IImportDeclaration} with the help of an {@link ImportRewrite} instance to refer to the origin
     * code instead to the Ecore code.
     */
    private void rewriteImport(IImportDeclaration importDeclaration, ImportRewrite implementationRewrite, ImportRewrite interfaceRewrite) {
        String oldName = importDeclaration.getElementName();
        String newName = nameUtil.cutFirstSegment(oldName); // generate new import string
        if (implementationRewrite.removeImport(oldName)) { // remove old import
            implementationRewrite.addImport(newName); // add to implementation class
            interfaceRewrite.removeImport(oldName); // remove old import as well
            interfaceRewrite.addImport(newName); // add to Ecore interface
        } else {
            logger.fatal("Could not remove Ecore import " + oldName);
        }
    }

    /**
     * Actually changes all the import declarations of the Ecore implementation class and the Ecore interface.
     */
    private void rewriteImports(ICompilationUnit ecoreImplementation, ICompilationUnit ecoreInterface) throws JavaModelException {
        ImportRewrite implementationRewrite = ImportRewrite.create(ecoreImplementation, true);
        ImportRewrite interfaceRewrite = ImportRewrite.create(ecoreInterface, true);
        for (IImportDeclaration importDeclaration : ecoreImplementation.getImports()) {
            if (isProblematic(importDeclaration)) { // edit every problematic import declaration
                rewriteImport(importDeclaration, implementationRewrite, interfaceRewrite);
                fixSelfImports(ecoreInterface, importDeclaration);
            }
        }
        ASTUtil.applyImportRewrite(ecoreImplementation, implementationRewrite, monitor);
        ASTUtil.applyImportRewrite(ecoreInterface, interfaceRewrite, monitor);
    }

    /**
     * Changes the imports of a compilation unit and its Ecore interface if it is an Ecore implementation class and the
     * imports of types in the same package if it is an Ecore interface. The super interface declarations of the classes
     * are retained, while the import declarations of the Ecore type are changed to the relating types of the origin
     * code.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        manipulateEcoreClass(unit);
        manipulateEcoreInterface(unit);
    }

}