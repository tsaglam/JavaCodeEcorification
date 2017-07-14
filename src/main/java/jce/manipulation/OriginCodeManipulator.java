package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import jce.util.PackageFilter;
import jce.util.ResourceRefresher;

/**
 * Base class for manipulating origin code.
 * @author Timur Saglam
 */
public abstract class OriginCodeManipulator {
    protected static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());
    protected String ecorePackage;
    protected String wrapperPackage;

    /**
     * Simple constructor that sets the package names.
     * @param ecorePackageName is the name of the Ecore code base package.
     * @param wrapperPackageName is the name of the wrapper code base package.
     */
    public OriginCodeManipulator(String ecorePackage, String wrapperPackage) {
        this.ecorePackage = ecorePackage;
        this.wrapperPackage = wrapperPackage;
    }

    /**
     * Manipulates the origin code of the given {@link IProject}.
     * @param project is the given {@link IProject}.
     */
    public void manipulate(IProject project) {
        logger.info("Starting the origin code manipulation " + getClass().getSimpleName());
        ResourceRefresher.refresh(project);
        try {
            for (IPackageFragment fragment : PackageFilter.startsNotWith(project, ecorePackage, wrapperPackage)) {
                if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    manipulate(fragment);
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Executes the origin code manipulation for every package.
     * @param fragment is the {@link IPackageFragment} of the package.
     * @throws JavaModelException if there are problems with the Java model.
     */
    protected abstract void manipulate(IPackageFragment fragment) throws JavaModelException;

    /**
     * Reads a {@link ICompilationUnit} and creates the AST DOM for manipulating the Java source file.
     * @param unit is the {@link ICompilationUnit}.
     * @return the {@link ASTNode}.
     */
    protected CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}