package jce.codemanipulation.origin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import jce.properties.EcorificationProperties;
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
}