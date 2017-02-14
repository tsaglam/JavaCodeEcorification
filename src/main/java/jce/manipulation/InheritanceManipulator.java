package jce.manipulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaProject;

/**
 * Changes the inheritance of the original Java classes.
 * @author Timur Saglam
 */
public class InheritanceManipulator {
    private static final Logger logger = LogManager.getLogger(InheritanceManipulator.class.getName());

    public void debug(IJavaProject project) throws JavaModelException { // TODO (MEDIUM) remove debug
        for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
            if (root.getElementName().equals("xtend-gen")) {
                System.err.println(root.getElementName());
                System.err.println("kind: " + root.getKind());
                System.err.println("children: " + root.getChildren().length);
                IPackageFragmentRoot[] roots = new IPackageFragmentRoot[] { root };
                for (IPackageFragment frag : ((JavaProject) project).getPackageFragmentsInRoots(roots)) {
                    if (frag.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        System.err.println("   name:" + frag.getElementName());
                        System.err.println("   kind: " + frag.getKind());
                        System.err.println("   children: " + frag.getChildren().length);
                        for (ICompilationUnit unit : frag.getCompilationUnits()) {
                            System.err.println("      " + unit.getElementName());
                        }
                    }
                }
            }
        }
        System.err.println("");
        for (IPackageFragment frag : project.getPackageFragments()) {
            if (frag.getKind() == IPackageFragmentRoot.K_SOURCE) {
                System.err.println("   name:" + frag.getElementName() + " has " + frag.getCompilationUnits().length);
            }
        }
    }

    /**
     * Changes the inheritance for all classes of specific packages.
     * @param packages are the specific packages.
     * @param project is the {@link IProject} that contains the packages.
     */
    public void manipulate(IPackageFragment[] packages, IProject project) {
        IJavaProject javaProject = getCompiled(project);
        try {
            debug(javaProject);
            for (IPackageFragment mypackage : packages) {
                if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    editTypesIn(mypackage, javaProject);
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
    private void editTypesIn(IPackageFragment myPackage, IJavaProject project) throws JavaModelException {
        TypeVisitor visitor = new TypeVisitor(myPackage.getElementName(), project);
        for (ICompilationUnit unit : myPackage.getCompilationUnits()) {
            CompilationUnit parse = parse(unit);
            parse.accept(visitor);
        }
    }

    private IJavaProject getCompiled(IProject project) {
        try {
            project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        } catch (CoreException exception) {
            logger.error(exception);
        }
        return JavaCore.create(project);
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