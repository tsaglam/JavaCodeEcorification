package jce.codemanipulation.origin;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.properties.EcorificationProperties;
import jce.util.RawTypeUtil;
import jce.util.jdt.ASTUtil;

/**
 * This class generates default constructors for every class that does not support default constructors. This is a
 * unstable fix for the problem that Ecore factories require default constructors to create instances.
 * @author Timur Saglam
 */
public class DefaultConstructorGenerator extends OriginCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public DefaultConstructorGenerator(EcorificationProperties properties) {
        super(properties);
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        ASTUtil.applyVisitorModifications(unit, new ConstructorGenerationVisitor(), monitor);
    }

    /**
     * {@link ASTVisitor} that generates default constructors if they are missing.
     * @author Timur Saglam
     */
    private class ConstructorGenerationVisitor extends ASTVisitor {

        @Override
        public boolean visit(TypeDeclaration node) {
            if (node.isPackageMemberTypeDeclaration() && hasConstructor(node)) {
                if (hasDefaultConstructor(node)) {
                    ensureVisibility(node); // make sure default constructor is public
                } else {
                    addDefaultConstructor(node); // add new default constructor
                }
            }
            return false;
        }

        /**
         * Adds a new public default constructor with an empty body to a {@link TypeDeclaration}.
         */
        @SuppressWarnings("unchecked")
        private void addDefaultConstructor(TypeDeclaration node) {
            AST ast = node.getAST();
            MethodDeclaration constructor = ast.newMethodDeclaration(); // create constructor
            constructor.setConstructor(true); // differentiate from normal methods
            constructor.setName(ast.newSimpleName(node.getName().getIdentifier())); // set name
            constructor.setBody(ast.newBlock()); // add empty method body
            constructor.modifiers().add(ast.newModifier(PUBLIC_KEYWORD)); // make public
            node.bodyDeclarations().add(constructor); // add to node
        }

        /**
         * Checks whether the default constructor of a {@link TypeDeclaration} is public, makes it public if it is not.
         */
        @SuppressWarnings("unchecked")
        private void ensureVisibility(TypeDeclaration node) {
            MethodDeclaration constructor = getDefaultConstructor(node);
            if (!Modifier.isPublic(constructor.getModifiers())) {
                removeModifiers(constructor);
                Modifier modifier = node.getAST().newModifier(PUBLIC_KEYWORD);
                constructor.modifiers().add(modifier); // make public
            }
        }

        /**
         * Finds the explicit default constructor of a {@link TypeDeclaration}, by finding the method that both is a
         * constructor and has no parameters. Returns null if no default constructor was found.
         */
        private MethodDeclaration getDefaultConstructor(TypeDeclaration type) {
            for (MethodDeclaration method : type.getMethods()) {
                if (method.isConstructor() && method.parameters().isEmpty()) {
                    return method;
                }
            }
            return null;
        }

        /**
         * Checks whether a {@link TypeDeclaration} has at least one constructor.
         */
        private boolean hasConstructor(TypeDeclaration type) {
            for (MethodDeclaration method : type.getMethods()) {
                if (method.isConstructor()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether a type has a explicit default constructor by checking whether one of its methods both is a
         * constructor and has no parameters.
         */
        private boolean hasDefaultConstructor(TypeDeclaration type) {
            return getDefaultConstructor(type) != null;
        }

        /**
         * Removes private and protected keyword in form of a {@link Modifier} from a type declaration node. Returns the
         * removed {@link Modifier} or null of none was removed.
         */ // TODO (HIGH) remove duplicate code with ClassExpositionVisitor
        private Modifier removeModifiers(BodyDeclaration node) {
            for (IExtendedModifier extendedModifier : RawTypeUtil.castList(IExtendedModifier.class, node.modifiers())) {
                if (extendedModifier.isModifier()) { // if is modifier (not annotation)
                    Modifier modifier = (Modifier) extendedModifier; // cast to modifier
                    if (modifier.isPrivate() || modifier.isProtected()) {
                        modifier.delete(); // remove modifier from node
                        return modifier;
                    }
                }
            }
            return null;
        }
    }
}