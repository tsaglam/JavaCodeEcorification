package jce.manipulation.ecorecode;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
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
    private final PathHelper path;

    /**
     * Basic constructor.
     * @param currentPackage is the current package.
     */
    public InterfaceRetentionVisitor(String currentPackage) {
        this.currentPackage = currentPackage;
        path = new PathHelper('.');
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            changeSuperInterface(node);
        }
        return super.visit(node);
    }

    /**
     * Changes the super interface reference of a {@link TypeDeclaration}.
     */
    @SuppressWarnings("unchecked")
    private void changeSuperInterface(TypeDeclaration declaration) {
        SimpleType ecoreInterface = getEcoreInterface(declaration);
        AST ast = declaration.getAST();
        String newName = path.append(path.cutLastSegment(currentPackage), ecoreInterface.getName().getFullyQualifiedName());
        Type newSuperType = ast.newSimpleType(ast.newName(newName));
        declaration.superInterfaceTypes().remove(ecoreInterface);
        declaration.superInterfaceTypes().add(newSuperType); // TODO (HIGH) Type safety warning
    }

    /**
     * Returns the simple type of the Ecore interface.
     */
    private SimpleType getEcoreInterface(TypeDeclaration declaration) {
        List<Type> superInterfaces = RawTypeUtil.castList(Type.class, declaration.superInterfaceTypes());
        for (Type type : superInterfaces) { // Search interfaces for Ecore interface.
            if (type.isSimpleType() && isEcoreInterface((SimpleType) type, declaration)) {
                return (SimpleType) type; // return type casted to simple type.
            }
        }
        return null;
    }

    /**
     * Checks whether a {@link SimpleType} is the Ecore interface of an {@link TypeDeclaration} which is an Ecore
     * implementation class.
     */
    private boolean isEcoreInterface(SimpleType superInterface, TypeDeclaration implementation) {
        String interfaceName = superInterface.getName().getFullyQualifiedName();
        interfaceName = path.getLastSegment(interfaceName);
        String implementationName = implementation.getName().getFullyQualifiedName();
        implementationName = path.getLastSegment(implementationName);
        return interfaceName.equals(implementationName.substring(0, implementationName.length() - 4));
    }

}