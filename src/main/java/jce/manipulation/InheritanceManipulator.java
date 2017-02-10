package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InheritanceManipulator {
    private static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());

    public void editPackages(IPackageFragment[] packages) {
        try {
            for (IPackageFragment mypackage : packages) {
                if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    createAST(mypackage);
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    private void createAST(IPackageFragment myPackage) throws JavaModelException {
        for (ICompilationUnit unit : myPackage.getCompilationUnits()) {
            CompilationUnit parse = parse(unit);
            TypeVisitor visitor = new TypeVisitor();
            parse.accept(visitor);
            for (TypeDeclaration type : visitor.getTypes()) {
                System.out.print("Type name: " + type.getName());
            }
        }
    }

    /**
     * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java source file
     * @param unit
     * @return
     */
    private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}