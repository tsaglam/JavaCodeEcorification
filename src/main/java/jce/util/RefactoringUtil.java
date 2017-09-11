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
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

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
     * @return the {@link RefactoringStatus} of the {@link CheckConditionsOperation}.
     */
    public static RefactoringStatus applyRefactoring(Refactoring refactoring, IProgressMonitor monitor) {
        CheckConditionsOperation conditionCheck = new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
        CreateChangeOperation changeCreator = new CreateChangeOperation(conditionCheck, RefactoringStatus.ERROR);
        PerformChangeOperation changePerformer = new PerformChangeOperation(changeCreator);
        try {
            ResourcesPlugin.getWorkspace().run(changePerformer, monitor);
        } catch (CoreException exception) {
            logger.fatal("Refactoring failed: " + refactoring.getName(), exception);
        }
        return conditionCheck.getStatus();
    }

    /**
     * Logs the messages of every {@link RefactoringStatusEntry} of an {@link RefactoringStatus} according their
     * severity.
     * @param status is the {@link RefactoringStatus}.
     * @param logger is the {@link Logger} which is used to log the messages.
     */
    public static void logStatus(RefactoringStatus status, Logger logger) {
        for (RefactoringStatusEntry entry : status.getEntries()) {
            if (entry.isFatalError()) {
                logger.fatal(entry.getMessage());
            } else if (entry.isError()) {
                logger.error(entry.getMessage());
            } else if (entry.isWarning()) {
                logger.warn(entry.getMessage());
            }
        }
    }
}
