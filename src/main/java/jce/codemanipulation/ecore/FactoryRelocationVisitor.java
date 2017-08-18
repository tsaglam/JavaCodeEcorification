package jce.codemanipulation.ecore;

import static jce.properties.TextProperty.FACTORY_PACKAGE;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import jce.properties.EcorificationProperties;

/**
 * Visitor that moves the original Ecore factory implementation class into a new subpackage.
 * @author Timur Saglam
 */
public class FactoryRelocationVisitor extends ASTVisitor {
    private final EcorificationProperties properties;

    /**
     * Basic constructor, sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FactoryRelocationVisitor(EcorificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        System.err.println(node.getName().getFullyQualifiedName()); // TODO (HIGH) remove debug output.
        AST ast = node.getAST();
        PackageDeclaration newPackage = ast.newPackageDeclaration();
        newPackage.setName(ast.newName(properties.get(FACTORY_PACKAGE)));
        // TODO (HIGH) add new package as a subpackage.
        return super.visit(node);
    }
}
