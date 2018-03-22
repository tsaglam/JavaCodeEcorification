package jce.codemanipulation.ecore;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.util.PathHelper;
import jce.util.RawTypeUtil;
import jce.util.jdt.TypeUtil;

/**
 * {@link ASTVisitor} class that retains the super interface declarations of the Ecore implementation classes. The
 * declaration is made independent from the imports of the class.
 * @author Timur Saglam
 */
public class InterfaceRetentionVisitor extends ASTVisitor {
    private final String currentPackage;
    private final IImportDeclaration[] imports;
    private final PathHelper pathHelper;

    /**
     * Basic constructor.
     * @param imports are the import declaration from which the full interface names are resolved.
     * @param currentPackage is the current package of the visited type.
     */
    public InterfaceRetentionVisitor(IImportDeclaration[] imports, String currentPackage) {
        super();
        if (imports == null || currentPackage == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        this.imports = imports;
        this.currentPackage = currentPackage;
        pathHelper = new PathHelper('.');
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        AST ast = node.getAST();
        List<Type> interfaces = RawTypeUtil.castList(Type.class, node.superInterfaceTypes());
        for (Type superInterface : interfaces) {
            changeSuperInterface(superInterface, node, ast);
        }
        return false;
    }

    /**
     * Changes the a super interface declaration of a {@link TypeDeclaration} to contain the fully qualified name.
     */
    @SuppressWarnings("unchecked")
    private void changeSuperInterface(Type oldInterface, TypeDeclaration node, AST ast) {
        SimpleType interfaceType = TypeUtil.getSimpleType(oldInterface);
        if (isNotEObject(interfaceType)) {
            String newName = getName(interfaceType);
            Type newSuperInterface = ast.newSimpleType(ast.newName(newName));
            newSuperInterface = copyParameters(oldInterface, newSuperInterface, ast);
            node.superInterfaceTypes().remove(oldInterface);
            node.superInterfaceTypes().add(newSuperInterface);
        }
    }

    /**
     * Adds all type parameters the old super interface to the new super interface.
     */
    @SuppressWarnings("unchecked")
    private Type copyParameters(Type oldInterface, Type newInterface, AST ast) {
        if (oldInterface.isParameterizedType()) { // if interface has parameters
            ParameterizedType parameterizedType = ast.newParameterizedType(newInterface); // parameterized new interface
            ParameterizedType castedType = (ParameterizedType) oldInterface; // cast old interface
            for (Type type : RawTypeUtil.castList(Type.class, castedType.typeArguments())) {
                type.delete(); // delete type parameter from old interface
                parameterizedType.typeArguments().add(type); // and add to the new one
            }
            return parameterizedType; // use parameterized type with copied parameters
        }
        return newInterface; // just use the original type
    }

    /**
     * Returns fully qualified name of a {@link SimpleType}. The name is resolved from the import declarations.
     */
    private String getName(SimpleType type) {
        String typeName = type.getName().getFullyQualifiedName();
        for (IImportDeclaration declaration : imports) {
            if (declaration.getElementName().endsWith(typeName)) {
                return declaration.getElementName();
            }
        }
        return pathHelper.append(currentPackage, typeName);
    }

    /**
     * Checks whether a type name matches the name of {@link EObject}.
     */
    private boolean isNotEObject(SimpleType type) {
        return !EObject.class.getSimpleName().equals(type.getName().getFullyQualifiedName());
    }
}