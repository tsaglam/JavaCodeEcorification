package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Encapsulates the fields of the origin code. This is necessary for the removal of the fields.
 * @author Timur Saglam
 */
public class FieldEncapsulator extends AbstractCodeManipulator {

    /**
     * Simple constructor that sets the properties.
     * @param properties are the {@link EcorificationProperties}.
     */
    public FieldEncapsulator(EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        ASTUtil.applyVisitorModifications(unit, new FieldUnfinalizationVisitor(), monitor); // make fields not final
        CompilationUnit parsedUnit = ASTUtil.parse(unit, monitor);
        parsedUnit.accept(new FieldEncapsulationVisitor(properties));
    }

    /**
     * {@link ASTVisitor} class that removes the modifier keyword final from all of its final fields.
     */
    private class FieldUnfinalizationVisitor extends ASTVisitor {
        @Override
        public boolean visit(TypeDeclaration node) {
            if (!node.isInterface() && node.isPackageMemberTypeDeclaration()) { // if is class
                for (FieldDeclaration field : node.getFields()) { // for every field:
                    if (Modifier.isFinal(field.getModifiers())) { // if is final
                        removeFinalKeyword(field); // remove final keyword
                    }
                }
            }
            return false;
        }

        /**
         * Removes the final keyword of an {@link BodyDeclaration}.
         */
        private void removeFinalKeyword(BodyDeclaration declaration) {
            IExtendedModifier finalModifier = null;
            for (Object object : declaration.modifiers()) { // search final modifier
                IExtendedModifier modifier = (IExtendedModifier) object; // API promises List<IExtendedModifier>
                if (modifier.isModifier() && ((Modifier) modifier).isFinal()) {
                    finalModifier = modifier; // remember final modifier
                }
            }
            declaration.modifiers().remove(finalModifier); // remove keyword
        }
    }
}