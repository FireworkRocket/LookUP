package org.fireworkrocket.lookup.kernel.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fireworkrocket.lookup.ui.exception.MemoryMonitor;

import java.util.concurrent.LinkedBlockingQueue;

public class ExceptionHandler {
    private static final int MAX_EXCEPTIONS = 100;
    private static final LinkedBlockingQueue<Throwable> EXCEPTIONS = new LinkedBlockingQueue<>(MAX_EXCEPTIONS);
    private static final Logger LOGGER = LogManager.getLogger(logCaller());

    public static void handleException(Throwable e) {
        if (e instanceof OutOfMemoryError) {
            MemoryMonitor.init();
        } else {
            if (EXCEPTIONS.size() >= MAX_EXCEPTIONS) {
                EXCEPTIONS.poll();
            }
            EXCEPTIONS.offer(e);
            LOGGER.error(logCaller(), e);
        }
    }

    public static void handleException(String message, Throwable e) {
        LOGGER.error(message, e);
    }

    public static void handleWarning(String message) {
        LOGGER.warn(message);
        logCaller();
    }

    public static void handleInfo(String message) {
        LOGGER.info(message);
        logCaller();
    }

    public static void handleDebug(String message) {
        LOGGER.debug(message);
        logCaller();
    }

    public static void handleFatal(String message, Throwable e) {
        LOGGER.fatal(message, e);
        logCaller();
    }

    public static void handleTrace(String message) {
        LOGGER.trace(message);
        logCaller();
    }

    private static String logCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            return (caller.getClassName() + "." + caller.getMethodName() + ":");
        }
        return null;
    }
}