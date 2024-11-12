package org.fireworkrocket.lookup.exception;

import io.github.palexdev.materialfx.enums.DialogType;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static org.fireworkrocket.lookup.Main.logFilename;
import static org.fireworkrocket.lookup.exception.MemoryMonitor.showAlert;

public class ExceptionHandler {
    private static final int MAX_EXCEPTIONS = 100;
    private static final LinkedBlockingQueue<Throwable> EXCEPTIONS = new LinkedBlockingQueue<>(MAX_EXCEPTIONS);
    private static final Logger LOGGER = LogManager.getLogger(logCaller());

    private static long lastTime;
    private static final Map<String, Runnable> BUTTONS = new HashMap<>();
    private static Set<Throwable> ex;

    public static void handleException(Throwable e) {
        if (e instanceof OutOfMemoryError) {
            MemoryMonitor.init();
        } else {
            if (EXCEPTIONS.size() >= MAX_EXCEPTIONS) {
                EXCEPTIONS.poll();
            }
            EXCEPTIONS.offer(e);

            dialogWindow("额，看起来并不顺利...", e);
            LOGGER.error(logCaller(), e);
        }
    }

    public static void handleException(String message, Throwable e) {
        dialogWindow(message, e);
        LOGGER.error(message, e);
    }

    private static void dialogWindow(String message, Throwable e) {
        new Thread(() -> {
            BUTTONS.put("查看日志", () -> {
                try {
                    File file = new File(ExceptionHandler.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "..\\..\\" + logFilename);
                    Runtime.getRuntime().exec("notepad "+file.getAbsolutePath());
                    System.out.println(file.getAbsolutePath());
                } catch (IOException ex) {
                    handleException(ex);
                }
            });
            BUTTONS.put("忽略", null);
            Platform.runLater(() -> DialogUtil.showDialog(DialogType.ERROR, "Error", message + "\n" + e.getMessage(), BUTTONS));
        }).start();
        lastTime = System.currentTimeMillis();
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