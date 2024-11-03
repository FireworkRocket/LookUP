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

/**
 * ExceptionHandler 类，用于处理各种类型的异常和日志消息。
 */
public class ExceptionHandler {
    private static final int MAX_EXCEPTIONS = 100;
    private static final LinkedBlockingQueue<Exception> EXCEPTIONS = new LinkedBlockingQueue<>(MAX_EXCEPTIONS);
    private static final Logger LOGGER = LogManager.getLogger(logCaller());

    private static long lastTime;
    private static final Map<String, Runnable> BUTTONS = new HashMap<>();
    private static Set<Exception> ex;

    /**
     * 处理异常，错误级别。
     *
     * @param e 要处理的异常
     */
    public static void handleException(Exception e) {
        if (EXCEPTIONS.size() >= MAX_EXCEPTIONS) {
            EXCEPTIONS.poll(); // 移除最旧的异常
        }
        EXCEPTIONS.offer(e);
        dialogWindow("额，看起来并不顺利...", e);
        LOGGER.error(logCaller(), e);
    }

    /**
     * 处理带有自定义消息的异常，错误级别。
     *
     * @param message 自定义消息
     * @param e       要处理的异常
     */
    public static void handleException(String message, Exception e) {
        dialogWindow(message, e);
        LOGGER.error(message, e);
    }

    /**
     * 显示带有异常消息的对话窗口。
     *
     * @param message 要显示的消息
     * @param e       要显示的异常
     */
    private static void dialogWindow(String message, Exception e) {
        new Thread(() -> {
            BUTTONS.put("查看日志", () -> {
                try {
                    File file = new File(ExceptionHandler.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "..\\..\\" + logFilename);
                    Runtime.getRuntime().exec(file.getAbsolutePath());
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

    /**
     * 处理带有自定义消息的警告。
     *
     * @param message 自定义消息
     */
    public static void handleWarning(String message) {
        LOGGER.warn(message);
        logCaller();
    }

    /**
     * 处理信息消息。
     *
     * @param message 信息消息
     */
    public static void handleInfo(String message) {
        LOGGER.info(message);
        logCaller();
    }

    /**
     * 处理调试消息。
     *
     * @param message 调试消息
     */
    public static void handleDebug(String message) {
        LOGGER.debug(message);
        logCaller();
    }

    /**
     * 处理致命错误。
     *
     * @param message 致命错误消息
     * @param e       要处理的异常
     */
    public static void handleFatal(String message, Exception e) {
        LOGGER.fatal(message, e);
        logCaller();
    }

    /**
     * 处理跟踪消息。
     *
     * @param message 跟踪消息
     */
    public static void handleTrace(String message) {
        LOGGER.trace(message);
        logCaller();
    }

    /**
     * 记录调用方法。
     *
     * @return 调用方法的信息
     */
    private static String logCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 调用方法在堆栈跟踪中的索引为3
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            return (caller.getClassName() + "." + caller.getMethodName() + ":");
        }
        return null;
    }
}