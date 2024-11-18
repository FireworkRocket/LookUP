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
import org.fireworkrocket.lookup.function.TrayIconManager;
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static org.fireworkrocket.lookup.Config.STOP_CHANGER_WALLPAPER;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.function.NetworkUtil.isConnected;
import static org.fireworkrocket.lookup.function.ProcessUtils.listProcesses;
import static org.fireworkrocket.lookup.function.ProcessUtils.setProcessSuspendable;

public class Main extends Application {
    private static ScheduledExecutorService service;
    private static ScheduledFuture<?> wallpaperChangerFuture;

    public static String logFilename = "Logs/Debug-" + System.currentTimeMillis() + ".log";

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> {
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
        if (isConnected()){
            stage.setTitle("Look UP!");
        } else {
            stage.setTitle("Look UP! - 离线模式");
        }

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

        if (!STOP_CHANGER_WALLPAPER) {
            wallpaperChangerFuture = getService().scheduleAtFixedRate(WallpaperChanger::getTodayWallpaper, 0, 1, TimeUnit.DAYS);
            new Thread(() -> new ListeningWallpaper().startListening()).start();
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

    public static void cancelWallpaperChangerTask() {
        if (wallpaperChangerFuture != null && !wallpaperChangerFuture.isCancelled()) {
            wallpaperChangerFuture.cancel(true);
        }
    }
}