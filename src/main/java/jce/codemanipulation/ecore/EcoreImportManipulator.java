package jce.codemanipulation.ecore;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.MetamodelSearcher;
import jce.util.PackageFilter;

/**
 * Base class for the adaption of problematic import declarations in the Ecore code. A problematic import declaration is
 * any import declaration that references a type from the Ecore package which has a counterpart in the Ecore metamodel
 * and the origin code. Unproblematic is any import declaration referencing Ecore package types or Ecore factory types.
 * This class basically changes all problematic imports in all Ecore implementation classes and adds the changed imports
 * to the correlating Ecore interfaces while retaining the correct super interfaces of the implementation classes.
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
        super(properties);
        this.metamodel = metamodel;
    }

    @Override
    public void manipulate(IProject project) {
        this.project = JavaCore.create(project); // makes the project instance available in this class.
        super.manipulate(project);
    }

    /**
     * Edits an {@link IImportDeclaration} with the help of an {@link ImportRewrite} instance to refer to the origin
     * code instead to the Ecore code.
     */
    private void edit(IImportDeclaration importDeclaration, ImportRewrite implementationRewrite, ImportRewrite interfaceRewrite) {
        String name = importDeclaration.getElementName();
        if (implementationRewrite.removeImport(name)) { // remove old import
            name = nameUtil.cutFirstSegment(name); // generate new import string
            implementationRewrite.addImport(name); // add to implementation class
            interfaceRewrite.removeImport(name);
            interfaceRewrite.addImport(name); // add to Ecore interface
        } else {
            logger.fatal("Could not remove Ecore import " + name);
        }
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
            return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) != null; // search metamodel counterpart
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
        return MetamodelSearcher.findEClass(typeName, metamodel.getRoot()) != null;
    }

    /**
     * Retains the super interface declarations of an compilation unit. .
     */
    private void retainInterface(ICompilationUnit unit) throws JavaModelException {
        applyVisitorModifications(unit, new InterfaceRetentionVisitor(unit.getImports(), unit.getParent().getElementName()));
    }

    @Override
    protected List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        return PackageFilter.startsWith(project, properties.get(TextProperty.ECORE_PACKAGE));
    }

    /**
     * Changes the imports of a compilation unit and its Ecore interface if it is an Ecore implementation class. The
     * super interface declarations of the classes are retained, while the import declarations of the Ecore type are
     * changed to the relating types of the origin code.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        if (isEcoreImplementation(unit)) { // is interface or implementation class of an EClass
            ICompilationUnit ecoreInterface = findEcoreInterface(unit); // get ecore interface
            retainInterface(unit); // retain the super interfaces of both
            retainInterface(ecoreInterface);
            ImportRewrite implementationRewrite = ImportRewrite.create(unit, true);
            ImportRewrite interfaceRewrite = ImportRewrite.create(ecoreInterface, true);
            for (IImportDeclaration importDeclaration : unit.getImports()) {
                if (isProblematic(importDeclaration)) { // edit every problematic import declaration
                    edit(importDeclaration, implementationRewrite, interfaceRewrite);
                }
            }
            applyImportRewrite(unit, implementationRewrite);
            applyImportRewrite(ecoreInterface, interfaceRewrite);
        }
    }
}