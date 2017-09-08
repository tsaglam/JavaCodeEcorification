package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} that makes all inner classes that have default visibility visible through changing the visibility
 * to public.
 * @author Timur Saglam
 */
public class ClassExpositionVisitor extends ASTVisitor {
    private static final Logger logger = LogManager.getLogger(ClassExpositionVisitor.class.getName());
    private IProgressMonitor monitor;

    /**
     * Basic constructor.
     * @param properties are the Ecorification properties.
     */
    public ClassExpositionVisitor(EcorificationProperties properties) {
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeDeclaration node) {
        if (isInnerClass(node) && isDefault(node)) { // if is default inner class
            AST ast = node.getAST();
            Modifier modifier = ast.newModifier(PUBLIC_KEYWORD); // create public modifier
            node.modifiers().add(modifier); // add to type declaration
            monitor.beginTask("Exposed: " + node.getName().getFullyQualifiedName(), 0);
        }
        return super.visit(node);
    }

    /**
     * Checks whether a {@link TypeDeclaration} is default and not static, which means it does not have the modifiers
     * public, private, protected or static.
     */
    private boolean isDefault(TypeDeclaration node) {
        int flags = node.getModifiers();
        return !Modifier.isPublic(flags) && !Modifier.isProtected(flags) && !Modifier.isPrivate(flags) && !Modifier.isStatic(flags);
    }

    /**
     * Checks whether a {@link TypeDeclaration} is a inner class, which means it is a member type and not an interface.
     */
    private boolean isInnerClass(TypeDeclaration node) {
        return node.isMemberTypeDeclaration() && !node.isInterface(); // is member class
    }
}