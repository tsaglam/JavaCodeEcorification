package jce.manipulation;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.util.PathHelper;
import jce.util.RawTypeUtil;;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations of the origin code.
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
     * @param declaration is the type declaration.
     * @param qualifiedName is the qualified name.
     */
    private void changeSuperInterface(TypeDeclaration declaration) {
        List<Type> superInterfaces = RawTypeUtil.castList(Type.class, declaration.superInterfaceTypes());
        SimpleType ecoreInterface = getEcoreInterface(superInterfaces, declaration);
        System.err.println("OLD: " + ecoreInterface.getName()); // TODO
        AST ast = declaration.getAST();
        String newName = path.append(path.cutLastSegment(currentPackage), ecoreInterface.getName().getFullyQualifiedName());
        System.err.println("NEW: " + newName); // TODO
        Type newSuperType = ast.newSimpleType(ast.newName(newName));
        superInterfaces.remove(ecoreInterface);
        superInterfaces.add(newSuperType);
    }

    private SimpleType getEcoreInterface(List<Type> superInterfaces, TypeDeclaration declaration) {
        for (Type type : superInterfaces) {
            if (type.isSimpleType()) { // TODO if is superclass of impl
                return (SimpleType) type;
            }
        }
        return null;
    }

}