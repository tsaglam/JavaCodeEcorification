package jce.codemanipulation.ecore;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.util.RawTypeUtil;

/**
 * {@link ASTVisitor} class that retains the super interface declarations of the Ecore implementation classes. The
 * declaration is made independent from the imports of the class.
 * @author Timur Saglam
 */
public class InterfaceRetentionVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(InterfaceRetentionVisitor.class.getName());
    private final IImportDeclaration[] imports;

    /**
     * Basic constructor.
     * @param imports are the import declaration from which the full interface names are resolved.
     */
    public InterfaceRetentionVisitor(IImportDeclaration[] imports) {
        this.imports = imports;
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
        if (superInterface.isSimpleType()) {
            SimpleType interfaceType = (SimpleType) superInterface;
            if (isNotEObject(interfaceType)) {
                String newName = getName(interfaceType);
                Type newSuperType = ast.newSimpleType(ast.newName(newName));
                node.superInterfaceTypes().remove(superInterface);
                node.superInterfaceTypes().add(newSuperType); // TODO (HIGH) Type safety warning
            }
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
        logger.fatal("Could not retain " + typeName + " because the related import was not found.");
        return typeName;
    } // TODO (HIGH) Fix detection of generic classes like CustomGenericClass<A,B>

    /**
     * Checks whether a type name matches the name of {@link EObject}.
     */
    private boolean isNotEObject(SimpleType type) {
        return !EObject.class.getSimpleName().equals(type.getName().getFullyQualifiedName());
    }
}