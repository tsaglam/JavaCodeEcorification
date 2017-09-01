package jce.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Utility class for applying LTK refactorings.
 * @author Timur Saglam
 */
public final class RefactoringUtil {
    private static final Logger logger = LogManager.getLogger(RefactoringUtil.class.getName());

    private RefactoringUtil() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Applies a {@link Refactoring} to the Workspace.
     * @param refactoring is the {@link Refactoring}.
     * @param monitor is the {@link IProgressMonitor} for this process.
     */
    public static void applyRefactoring(Refactoring refactoring, IProgressMonitor monitor) {
        CheckConditionsOperation conditionCheck = new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
        CreateChangeOperation changeCreator = new CreateChangeOperation(conditionCheck, RefactoringStatus.WARNING);
        PerformChangeOperation changePerformer = new PerformChangeOperation(changeCreator);
        try {
            ResourcesPlugin.getWorkspace().run(changePerformer, monitor);
        } catch (CoreException exception) {
            logger.fatal("Refactoring failed: " + refactoring.getName(), exception);
        }
    }
}
