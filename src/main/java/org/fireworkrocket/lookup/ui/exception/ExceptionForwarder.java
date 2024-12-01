package org.fireworkrocket.lookup.ui.exception;

import io.github.palexdev.materialfx.enums.DialogType;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fireworkrocket.lookup.Main.logFilename;

public class ExceptionForwarder {

    private static long lastTime;
    private static final Map<String, Runnable> BUTTONS = new HashMap<>();
    private static Set<Throwable> ex;

    public static void handleException(Throwable e) {
        dialogWindow("额，看起来并不顺利...", e);
        org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException(e);
    }

    public static void handleException(String message, Throwable e) {
        dialogWindow(message, e);
        org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException(e);
    }

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
