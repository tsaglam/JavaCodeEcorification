package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.RawTypeUtil;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} that makes all classes visible through changing the visibility to public.
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
        if (isHidden(node)) { // if should be exposed
            AST ast = node.getAST();
            if (isPrivate(node)) {
                removePrivateKeyword(node);
            }
            Modifier modifier = ast.newModifier(PUBLIC_KEYWORD); // create public modifier
            node.modifiers().add(modifier); // add to type declaration
            monitor.beginTask("Exposed: " + node.getName().getFullyQualifiedName(), 0);
        }
        return super.visit(node);
    }

    /**
     * Checks whether a {@link TypeDeclaration} is default or private and not an interface, which means it does not have
     * the modifiers public, protected or static.
     */
    private boolean isHidden(TypeDeclaration node) {
        int flags = node.getModifiers();
        return !node.isInterface() && !Modifier.isPublic(flags) && !Modifier.isProtected(flags);
    }

    /**
     * Checks whether a type declaration node is private.
     */
    private boolean isPrivate(TypeDeclaration node) {
        return Modifier.isPrivate(node.getModifiers());
    }

    /**
     * Removes private keyword from a type declaration node.
     */
    private void removePrivateKeyword(TypeDeclaration node) {
        for (IExtendedModifier extendedModifier : RawTypeUtil.castList(IExtendedModifier.class, node.modifiers())) {
            if (extendedModifier.isModifier()) { // if is modifier (not annotation)
                Modifier modifier = (Modifier) extendedModifier; // cast modifier
                if (modifier.isPrivate()) { // if is keyword private
                    modifier.delete(); // remove from node
                }
            }
        }
    }
}