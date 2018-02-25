package jce.codemanipulation;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import jce.properties.EcorificationProperties;
import jce.util.PathHelper;
import jce.util.ResourceRefresher;
import jce.util.jdt.PackageFilter;
import jce.util.logging.MonitorFactory;

/**
 * Base class for code manipulation. Can be extended for specific code manipulator classes. Offers functionality for
 * applying text edits and visitor modifications to any {@link ICompilationUnit}.
 * @author Timur Saglam
 */
public abstract class AbstractCodeManipulator {
    private String[] excludedPackages;
    private String packageName;
    protected Logger logger;
    protected final IProgressMonitor monitor;
    protected PathHelper nameUtil;
    protected final EcorificationProperties properties;

    /**
     * Simple constructor for the manipulation for multiple packages.
     * @param properties are the {@link EcorificationProperties}.
     * @param excludedPackages are the package prefixes whose packages should not be manipulated.
     */
    public AbstractCodeManipulator(EcorificationProperties properties, String... excludedPackages) {
        this.properties = properties;
        this.excludedPackages = excludedPackages;
        logger = LogManager.getLogger(this.getClass().getName());
        monitor = MonitorFactory.createProgressMonitor(logger, properties);
        nameUtil = new PathHelper('.');
    }

    /**
     * Simple constructor for the manipulation of one specific package.
     * @param packageName is the name of the specific package.
     * @param properties are the {@link EcorificationProperties}.
     */
    public AbstractCodeManipulator(String packageName, EcorificationProperties properties) {
        this(properties);
        this.packageName = packageName;
    }

    /**
     * Manipulates the code of the given {@link IProject}.
     * @param project is the given {@link IProject}.
     */
    public void manipulate(IProject project) {
        logger.info("Starting " + getClass().getSimpleName() + "...");
        ResourceRefresher.refresh(project);
        List<IPackageFragment> packages = filterPackages(project, properties);
        try {
            for (IPackageFragment fragment : packages) {
                if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    for (ICompilationUnit unit : fragment.getCompilationUnits()) {
                        manipulate(unit);
                    }
                }
            }
        } catch (JavaModelException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Defines the {@link IPackageFragment} list which will be manipulated. Either selects one specific package of
     * filters out a set of packages.
     * @param project is the {@link IProject} to manipulate.
     * @param properties are the {@link EcorificationProperties}.
     * @return the target packages of this code manipulator.
     */
    private List<IPackageFragment> filterPackages(IProject project, EcorificationProperties properties) {
        if (packageName == null) {
            return PackageFilter.startsNotWith(project, excludedPackages);
        }
        return PackageFilter.startsWith(project, packageName);
    }

    /**
     * Returns the name of the package member type of a compilation unit. E.g. "model.Main" from "Main.java"
     * @param unit is the {@link ICompilationUnit}.
     * @return the name as a String.
     * @throws JavaModelException if there are problems with the Java model.
     */
    protected String getPackageMemberName(ICompilationUnit unit) throws JavaModelException {
        String packageName = unit.getParent().getElementName();
        String memberName = nameUtil.cutLastSegment(unit.getElementName()); // cut the filename extension
        return nameUtil.append(packageName, memberName);
    }

    /**
     * Executes the origin code manipulation on a compilation unit.
     * @param unit is the {@link ICompilationUnit}.
     * @throws JavaModelException if there are problems with the Java model.
     */
    protected abstract void manipulate(ICompilationUnit unit) throws JavaModelException;
}