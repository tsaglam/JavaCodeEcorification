package jce.codemanipulation.origin;

import static jce.properties.TextProperty.ECORE_PACKAGE;
import static jce.properties.TextProperty.WRAPPER_PACKAGE;
import static jce.properties.TextProperty.WRAPPER_PREFIX;
import static jce.properties.TextProperty.WRAPPER_SUFFIX;

import org.apache.log4j.LogManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;

import jce.properties.EcorificationProperties;
import jce.util.PathHelper;
import jce.util.RawTypeUtil;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} class that manipulates the inheritance relations of the origin code.
 * @author Timur Saglam
 */
public class InheritanceManipulationVisitor extends ASTVisitor {
    private final String currentPackage;
    private final IProgressMonitor monitor;
    private final PathHelper nameUtil;
    private final EcorificationProperties properties;

    /**
     * Basic constructor.
     * @param currentPackage is the current package.
     * @param properties are the Ecorification properties.
     */
    public InheritanceManipulationVisitor(String currentPackage, EcorificationProperties properties) {
        this.currentPackage = currentPackage;
        this.properties = properties;
        monitor = MonitorFactory.createProgressMonitor(LogManager.getLogger(getClass().getName()), properties);
        nameUtil = new PathHelper('.');
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.isPackageMemberTypeDeclaration()) { // is no nested type
            if (node.isInterface()) {
                addEcoreInterface(node); // add correlating Ecore interface as super interface
            } else {
                overrideSuperclass(node); // set correlating wrapper as super class
            }
        }
        return false;
    }

    /**
     * Adds the correlating Ecore interface of an {@link TypeDeclaration} as super interface. Other superinterfaces are
     * being retained.
     */
    @SuppressWarnings("unchecked")
    private void addEcoreInterface(TypeDeclaration node) {
        String name = nameUtil.append(properties.get(ECORE_PACKAGE), currentPackage, node.getName().getIdentifier());
        node.superInterfaceTypes().add(nameToType(node, name));
        monitor.beginTask("Added Ecore super interface to " + node.getName().getIdentifier(), 0);
    }

    /**
     * Returns the name of the super class.
     */
    private String getWrapperName(TypeDeclaration node) {
        return properties.get(WRAPPER_PREFIX) + node.getName().getIdentifier() + properties.get(WRAPPER_SUFFIX);
    }

    /**
     * Builds an {@link SimpleType} out of an fully qualified name and a {@link TypeDeclaration}.
     */
    @SuppressWarnings("unchecked")
    private Type nameToType(TypeDeclaration declaration, String qualifiedName) {
        AST ast = declaration.getAST();
        Name name = ast.newName(qualifiedName);
        Type type = ast.newSimpleType(name);
        if (!declaration.typeParameters().isEmpty()) { // TODO (MEDIUM) optimize this, reduce duplication
            ParameterizedType parameterizedType = ast.newParameterizedType(type);
            for (TypeParameter parameter : RawTypeUtil.castList(TypeParameter.class, declaration.typeParameters())) {
                Name parameterName = ast.newSimpleName(parameter.getName().getIdentifier());
                parameterizedType.typeArguments().add(ast.newSimpleType(parameterName));
            }
            type = parameterizedType;
        }

        return type;
    }

    /**
     * Sets the correlating wrapper class of an {@link TypeDeclaration} as the super class. This overrides the old super
     * class.
     */
    private void overrideSuperclass(TypeDeclaration node) {
        String name = nameUtil.append(properties.get(WRAPPER_PACKAGE), currentPackage, getWrapperName(node));
        node.setSuperclassType(nameToType(node, name));
        monitor.beginTask("Changed super type of " + node.getName().getIdentifier(), 0);
    }

}