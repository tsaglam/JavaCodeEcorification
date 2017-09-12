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
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * Utility class for applying any LTK {@link Refactoring}.
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
     * @param failureLevel the severity from which on the condition checking is interpreted as failed and the
     * {@link Refactoring} not applied. The passed value must be greater than {@link RefactoringStatus#OK} and less than
     * or equal {@link RefactoringStatus#FATAL}. The standard value from which on a condition check should is to be
     * interpreted as failed can be accessed via {@link RefactoringCore#getConditionCheckingFailedSeverity()}.
     * @param monitor is the {@link IProgressMonitor} for this process.
     * @return the {@link RefactoringStatus} of the {@link CheckConditionsOperation}.
     */
    public static RefactoringStatus applyRefactoring(Refactoring refactoring, int failureLevel, IProgressMonitor monitor) {
        int conditions = CheckConditionsOperation.ALL_CONDITIONS; // check on all conditions
        CheckConditionsOperation conditionCheck = new CheckConditionsOperation(refactoring, conditions);
        CreateChangeOperation changeCreator = new CreateChangeOperation(conditionCheck, failureLevel);
        PerformChangeOperation changePerformer = new PerformChangeOperation(changeCreator);
        try {
            ResourcesPlugin.getWorkspace().run(changePerformer, monitor);
        } catch (CoreException exception) {
            logger.fatal("Refactoring failed: " + refactoring.getName(), exception);
        }
        return conditionCheck.getStatus();
    }

    /**
     * Convenience method that applies a {@link Refactoring} to the Workspace. The severity from which on the condition
     * checking is interpreted as failed and the {@link Refactoring} not applied is {@link RefactoringStatus#ERROR}.
     * @param refactoring is the {@link Refactoring}.
     * @param monitor is the {@link IProgressMonitor} for this process.
     * @return the {@link RefactoringStatus} of the {@link CheckConditionsOperation}.
     */
    public static RefactoringStatus applyRefactoring(Refactoring refactoring, IProgressMonitor monitor) {
        return applyRefactoring(refactoring, RefactoringStatus.ERROR, monitor);
    }

    /**
     * Convenience method that applies a {@link Refactoring} to the Workspace and logs the messages of every
     * {@link RefactoringStatusEntry} of an {@link RefactoringStatus} according their severity. The severity from which
     * on the condition checking is interpreted as failed and the {@link Refactoring} not applied is
     * {@link RefactoringStatus#ERROR}.
     * @param refactoring is the {@link Refactoring}.
     * @param monitor is the {@link IProgressMonitor} for this process.
     * @param logger is the {@link Logger} which is used to log the messages.
     * @return the {@link RefactoringStatus} of the {@link CheckConditionsOperation}.
     */
    public static RefactoringStatus applyRefactoring(Refactoring refactoring, IProgressMonitor monitor, Logger logger) {
        RefactoringStatus status = applyRefactoring(refactoring, monitor);
        logStatus(status, logger);
        return status;
    }

    /**
     * Logs the messages of every {@link RefactoringStatusEntry} of an {@link RefactoringStatus} according their
     * severity.
     * @param status is the {@link RefactoringStatus}.
     * @param logger is the {@link Logger} which is used to log the messages.
     */
    public static void logStatus(RefactoringStatus status, Logger logger) {
        for (RefactoringStatusEntry entry : status.getEntries()) {
            if (entry.isFatalError()) { // log fatal status as fatal level:
                logger.fatal(entry.getMessage());
            } else if (entry.isError()) { // log fatal status as error level:
                logger.error(entry.getMessage());
            } else if (entry.isWarning()) { // warnings can generally be ignored
                logger.debug(entry.getMessage()); // log as debug level
            }
        }
    }
}
