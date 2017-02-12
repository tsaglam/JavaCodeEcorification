package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator {
    private static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());

    /**
     * Changes the inheritance for all classes of specific packages.
     * @param packages are the specific packages.
     */
    public void manipulate(IPackageFragment[] packages) {
        try {
            for (IPackageFragment mypackage : packages) {
                if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    editTypesIn(mypackage);
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Visits all types of all {@link ICompilationUnit}s of a {@link IPackageFragment}.
     * @param myPackage is the {@link IPackageFragment}.
     * @throws JavaModelException if there is a problem with the JDT API.
     */
    private void editTypesIn(IPackageFragment myPackage) throws JavaModelException {
        TypeVisitor visitor = new TypeVisitor(myPackage.getJavaProject());
        for (ICompilationUnit unit : myPackage.getCompilationUnits()) {
            CompilationUnit parse = parse(unit);
            parse.accept(visitor);
        }
    }

    /**
     * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
     * @param unit is the {@link ICompilationUnit}.
     * @return the {@link ASTNode}.
     */
    private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}