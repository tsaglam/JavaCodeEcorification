package jce.codemanipulation.ecore;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * AST visitor that resolves the name of the package member type of the compilation unit.
 * @author Timur Saglam
 */
public class TypeNameResolver extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(TypeNameResolver.class.getName());
    private String typeName;

    /**
     * Getter for the resolved type name.
     * @return the fully qualified name of the resolved type binding of the type declaration.
     */
    public String getTypeName() {
        if (typeName == null) {
            throw new IllegalStateException("Visit a compilation unit before retrieving the name.");
        }
        return typeName;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        resolve(node);
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        resolve(node);
        return false;
    }

    /**
     * Visits a {@link TypeDeclaration} or {@link EnumDeclaration} and resolves the name.
     */
    private void resolve(AbstractTypeDeclaration type) {
        if (type.isPackageMemberTypeDeclaration()) {
            typeName = type.getName().resolveTypeBinding().getQualifiedName(); // fully qualified name of class
            if (typeName == null || "".equals(typeName)) {
                logger.fatal("Could not resolve fully qualified name of " + type.getName());
            }
        }
    }
}