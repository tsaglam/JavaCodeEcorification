package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.RawTypeUtil;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} that makes all hidden classes visible through changing the visibility to public. Hidden classes
 * are classes that were only referenced from a certain scope in the original code, but are now referenced from a
 * different scope. These are either default classes which are now referenced from another package (because of the
 * package structure of the Ecore code) or default/private member classes that are now referenced from another class
 * (e.g. the Ecore interface or implementation class) or package.
 * @author Timur Saglam
 */
class ClassExpositionVisitor extends ASTVisitor {
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
            removeKeywords(node); // remove private and protected keywords
            Modifier modifier = node.getAST().newModifier(PUBLIC_KEYWORD); // create public modifier
            node.modifiers().add(modifier); // add to type declaration
            monitor.beginTask("Exposed: " + node.getName().getFullyQualifiedName(), 0);
        }
        return super.visit(node);
    }

    /**
     * Checks whether a {@link TypeDeclaration} node a hidden class, which means it is either a default class or a
     * default/private member class.
     */
    private boolean isHidden(TypeDeclaration node) {
        return !node.isInterface() && (isHiddenClass(node) || isHiddenMemberClass(node));
    }

    /**
     * Checks whether a {@link TypeDeclaration} node a hidden class, which means it is a default class.
     */
    private boolean isHiddenClass(TypeDeclaration node) {
        return node.isPackageMemberTypeDeclaration() && !Modifier.isPublic(node.getModifiers());
    }

    /**
     * Checks whether a {@link TypeDeclaration} node a hidden member class, which means it is a default or private
     * member class.
     */
    private boolean isHiddenMemberClass(TypeDeclaration node) {
        return node.isMemberTypeDeclaration() && !Modifier.isPublic(node.getModifiers());
    }

    /**
     * Removes private and protected keyword from a type declaration node.
     */
    private void removeKeywords(TypeDeclaration node) {
        for (IExtendedModifier extendedModifier : RawTypeUtil.castList(IExtendedModifier.class, node.modifiers())) {
            if (extendedModifier.isModifier()) { // if is modifier (not annotation)
                Modifier modifier = (Modifier) extendedModifier; // cast modifier
                if (modifier.isPrivate() || modifier.isProtected()) { // if is keyword private
                    modifier.delete(); // remove from node
                }
            }
        }
    }
}