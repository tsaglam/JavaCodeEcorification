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

// TODO (MEDIUM) add JDoc and inline comments.
public class InheritanceManipulator {
    private static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());

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
        TypeVisitor visitor = new TypeVisitor();
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