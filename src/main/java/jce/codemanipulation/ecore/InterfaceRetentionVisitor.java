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
    private void changeSuperInterface(Type superInterface, TypeDeclaration node, AST ast) {
        SimpleType interfaceType = getSimpleType(superInterface);
        if (isNotEObject(interfaceType)) {
            String newName = getName(interfaceType);
            Type newSuperType = ast.newSimpleType(ast.newName(newName));
            node.superInterfaceTypes().remove(superInterface);
            node.superInterfaceTypes().add(newSuperType);
        }
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
     * Returns the {@link SimpleType} of a {@link Type}. If the {@link Type} is a {@link SimpleType} it is simply
     * casted. If it is a {@link ParameterizedType}, its type gets resolved recusivley with this method.
     */
    private SimpleType getSimpleType(Type type) {
        if (type.isSimpleType()) { // cast simple types
            return (SimpleType) type;
        } else if (type.isParameterizedType()) { // get simple types from parameterized types.
            return getSimpleType(((ParameterizedType) type).getType());
        } // not supported: primitive types, array types
        throw new IllegalArgumentException("Cannot get simple type from  " + type);
    }

    /**
     * Checks whether a type name matches the name of {@link EObject}.
     */
    private boolean isNotEObject(SimpleType type) {
        return !EObject.class.getSimpleName().equals(type.getName().getFullyQualifiedName());
    }
}