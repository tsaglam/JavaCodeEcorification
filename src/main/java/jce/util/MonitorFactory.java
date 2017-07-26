package jce.util;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Monitor;

import jce.properties.BinaryProperty;
import jce.properties.EcorificationProperties;

/**
 * Utility class for the creation of monitors.
 * @author Timur Saglam
 */
public final class MonitorFactory {

    private MonitorFactory() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates an {@link Monitor} according to the {@link EcorificationProperties}. This can either be a working monitor
     * that feeds into the {@link Logger} or a {@link BasicMonitor}. A working monitor is created when full logging is
     * enabled.
     * @param logger is the {@link Logger}.
     * @param properties are the {@link EcorificationProperties}.
     * @return the monitor.
     */
    public static Monitor createMonitor(Logger logger, EcorificationProperties properties) {
        if (properties.get(BinaryProperty.FULL_LOGGING)) { // if full logging is enabled:
            return new MonitorAdapter(logger); // create real logger.
        }
        return new BasicMonitor(); // else: create null logger.
    }

    /**
     * Creates an {@link IProgressMonitor} according to the {@link EcorificationProperties}. This can either be a
     * working progress monitor that feeds into the {@link Logger} or a {@link NullProgressMonitor}. A working monitor
     * is created when full logging is enabled.
     * @param logger is the {@link Logger}.
     * @param properties are the {@link EcorificationProperties}.
     * @return the monitor.
     */
    public static IProgressMonitor createProgressMonitor(Logger logger, EcorificationProperties properties) {
        if (properties.get(BinaryProperty.FULL_LOGGING)) { // if full logging is enabled:
            return new ProgressMonitorAdapter(logger); // create real logger.
        }
        return new NullProgressMonitor(); // else: create null logger.
    }
}
