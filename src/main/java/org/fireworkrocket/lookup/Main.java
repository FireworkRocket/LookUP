package org.fireworkrocket.lookup;

import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fireworkrocket.lookup.exception.DialogUtil;
import org.fireworkrocket.lookup.exception.MemoryMonitor;
import org.fireworkrocket.lookup.function.wallpaperchanger.ListeningWallpaper;
import org.fireworkrocket.lookup.function.wallpaperchanger.TrayIconManager;
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.function.ProcessUtils.listProcesses;
import static org.fireworkrocket.lookup.function.ProcessUtils.setProcessSuspendable;

public class Main extends Application {
    private static ScheduledExecutorService service;

    public static String logFilename = "Logs/Debug-" + System.currentTimeMillis() + ".log";

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof OutOfMemoryError) {
                EventQueue.invokeLater(() -> MemoryMonitor.showAlert(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
            } else {
                handleException(throwable);
            }
        });

        System.setProperty("logFilename", logFilename);
        System.setProperty("log4j.configurationFile", Objects.requireNonNull(Main.class.getResource("log4j2.xml")).toString());
        int[] processes = listProcesses();
        for (int pid : processes) {
            setProcessSuspendable(pid);
        }

        new Thread(() -> {
            ListeningWallpaper listeningWallpaper = new ListeningWallpaper();
            listeningWallpaper.startListening();
        });

        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (!SystemTray.isSupported()) {
            handleException(new Exception("System tray not supported!"));
            Platform.exit();
            return;
        }

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Home.fxml")));

        Scene scene = new Scene(root);
        stage.setTitle("Look UP!");
        stage.getIcons().add(new javafx.scene.image.Image(Objects.requireNonNull(Main.class.getResource("icon.png")).toString()));
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();
        stage.setScene(scene);
        stage.show();

        boolean stopChanger = true;
        if (!stopChanger) {
            getService().scheduleAtFixedRate(WallpaperChanger::getTodayWallpaper, 0, 1, TimeUnit.DAYS);
        }

        getService().scheduleAtFixedRate(System::gc, 0, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(getService()::shutdown));

        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(() -> {
            try {
                TrayIconManager.initializeTrayIcon(stage);
            } catch (AWTException e) {
                handleException(e);
            }
        });

        stage.setOnCloseRequest(event -> {
            Map<String, Runnable> buttons = new HashMap<>();
            buttons.put("是", () -> {
                event.consume();
                stage.hide();
            });
            buttons.put("否", () -> new Thread(() -> Platform.runLater(() -> {
                getService().shutdown();
                System.exit(0);
            })).start());
            DialogUtil.showDialog(DialogType.INFO, "关闭", "最小化到托盘？", buttons);
        });
    }

    public static ScheduledExecutorService getService() {
        if (service == null) {
            service = Executors.newScheduledThreadPool(1);
        }
        return service;
    }
}