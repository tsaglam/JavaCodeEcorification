package jce.codemanipulation.origin;

import static jce.properties.TextProperty.WRAPPER_PACKAGE;
import static jce.properties.TextProperty.WRAPPER_PREFIX;
import static jce.properties.TextProperty.WRAPPER_SUFFIX;

import org.apache.log4j.LogManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} class that manipulates the inheritance relations of the origin code.
 * @author Timur Saglam
 */
public class InheritanceManipulationVisitor extends ASTVisitor {
    private final String currentPackage;
    private final EcorificationProperties properties;
    private final IProgressMonitor monitor;

    /**
     * Basic constructor.
     * @param currentPackage is the current package.
     * @param properties are the Ecorification properties.
     */
    public InheritanceManipulationVisitor(String currentPackage, EcorificationProperties properties) {
        this.currentPackage = currentPackage;
        this.properties = properties;
        monitor = MonitorFactory.createProgressMonitor(LogManager.getLogger(getClass().getName()), properties);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // if is class, manipulate inheritance:
            setSuperClass(node, getPackage() + "." + getName(node));
            monitor.beginTask("Changed super type of " + node.getName().getIdentifier(), 0);
        }
        return false;
    }

    /**
     * Returns the name of the super class.
     */
    private String getName(TypeDeclaration node) {
        return properties.get(WRAPPER_PREFIX) + node.getName().toString() + properties.get(WRAPPER_SUFFIX);
    }

    /**
     * Returns the package path of the super class.
     */
    private String getPackage() {
        return properties.get(WRAPPER_PACKAGE) + "." + currentPackage;
    }

    /**
     * Sets a qualified name as super class of a type declaration.
     * @param declaration is the type declaration.
     * @param qualifiedName is the qualified name.
     */
    private void setSuperClass(TypeDeclaration declaration, String qualifiedName) {
        AST ast = declaration.getAST();
        Name name = ast.newName(qualifiedName);
        Type type = ast.newSimpleType(name);
        declaration.setSuperclassType(type);
    }

}