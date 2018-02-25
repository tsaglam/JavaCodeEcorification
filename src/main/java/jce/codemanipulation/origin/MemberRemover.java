package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import eme.generator.GeneratedEcoreMetamodel;
import jce.codemanipulation.AbstractCodeManipulator;
import jce.properties.EcorificationProperties;
import jce.properties.TextProperty;
import jce.util.jdt.ASTUtil;

/**
 * Removes all private non-static fields and their access methods from the origin code that have a counterpart in the
 * Ecore metamodel which was extracted from the origin code.
 * @author Timur Saglam
 */
public class MemberRemover extends AbstractCodeManipulator {
    private GeneratedEcoreMetamodel metamodel;

    /**
     * Simple constructor that sets the properties.
     * @param metamodel is the Ecore metamodel which was extracted from the origin code.
     * @param properties are the {@link EcorificationProperties}.
     */
    public MemberRemover(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        super(properties, properties.get(TextProperty.ECORE_PACKAGE), properties.get(TextProperty.WRAPPER_PACKAGE));
        this.metamodel = metamodel;
    }

    @Override
    protected void manipulate(ICompilationUnit unit) throws JavaModelException {
        ASTUtil.applyVisitorModifications(unit, new MemberRemovalVisitor(metamodel, properties), monitor);
    }
}