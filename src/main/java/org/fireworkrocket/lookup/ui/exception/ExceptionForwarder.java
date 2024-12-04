package org.fireworkrocket.lookup.ui.exception;

import io.github.palexdev.materialfx.enums.DialogType;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fireworkrocket.lookup.Main.logFilename;

/**
 * 异常转发器类，用于处理和显示异常信息。
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * try {
 *     // 可能抛出异常的代码
 * } catch (Exception e) {
 *     ExceptionForwarder.handleException(e);
 * }
 * }</pre>
 */
public class ExceptionForwarder {

    private static long lastTime;
    private static final Map<String, Runnable> BUTTONS = new HashMap<>();
    private static Set<Throwable> ex;

    /**
     * 处理异常并显示对话框。
     *
     * @param e 要处理的异常
     */
    public static void handleException(Throwable e) {
        dialogWindow("额，看起来并不顺利...", e);
        org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException(e);
    }

    /**
     * 处理异常并显示自定义消息的对话框。
     *
     * @param message 自定义消息
     * @param e 要处理的异常
     */
    public static void handleException(String message, Throwable e) {
        dialogWindow(message, e);
        org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException(e);
    }

    /**
     * 显示异常信息的对话框。
     *
     * @param message 对话框消息
     * @param e 要显示的异常
     */
    public static void dialogWindow(String message, Throwable e) {
        new Thread(() -> {
            BUTTONS.put("查看日志", () -> {
                try {
                    File file = new File(org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "..\\..\\" + logFilename);
                    Runtime.getRuntime().exec("notepad " + file.getAbsolutePath());
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

}