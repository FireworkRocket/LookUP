package org.fireworkrocket.lookup;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import org.apache.commons.cli.*;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.config.UserConfigImpl;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;
import org.fireworkrocket.lookup.ui.exception.DialogUtil;
import org.fireworkrocket.lookup.ui.exception.MemoryMonitor;
import org.fireworkrocket.lookup.ui.wallpaper.ListeningWallpaper;
import org.fireworkrocket.lookup.ui.TrayIconManager;
import org.fireworkrocket.lookup.ui.wallpaper.WallpaperChanger;
import org.fireworkrocket.lookup.kernel.config.DatabaseUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static org.fireworkrocket.lookup.kernel.config.DefaultConfig.stop_Changer_Wallpaper;
import static org.fireworkrocket.lookup.kernel.process.net.util.NetworkUtil.isConnected;
import static org.fireworkrocket.lookup.ui.ProcessUtils.listProcesses;
import static org.fireworkrocket.lookup.ui.ProcessUtils.setProcessSuspendable;
import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException;

public class Main extends Application {
    private static ScheduledExecutorService service;
    private static ScheduledFuture<?> wallpaperChangerFuture;

    public static final String logFilename = "Logs/Debug-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".log";

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            Options options = new Options();

            Option offlineOption = new Option("offline", "run in offline mode");
            options.addOption(offlineOption);

            Option importOption = new Option("import", true, "import data");
            options.addOption(importOption);

            CommandLineParser parser = new DefaultParser();
            try {
                CommandLine cmd = parser.parse(options, args);

                if (cmd.hasOption("offline")) {
                    DefaultConfig.checkConnected = false;
                }

                if (cmd.hasOption("import")) {
                    String importValue = cmd.getOptionValue("import");
                    System.out.println("Import value: " + importValue);
                    if (importValue != null) {
                        processImportValue(importValue);
                    } else {
                        ExceptionHandler.handleException(new Throwable("No import value provided"));
                    }
                }
            } catch (ParseException e) {
                ExceptionHandler.handleException(new IllegalArgumentException("Failed to parse command line arguments", e));
            }
            System.exit(0);
        }

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

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
           DatabaseUtil.deleteAllTemporaryUsers();
        }));

        new UserConfigImpl();
        launch();
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

        if (!stop_Changer_Wallpaper) {
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

    private static void processImportValue(String importValue) {
        try {
            URL url = new URI(importValue).toURL(); // 验证 URL

            // 验证返回的内容是否为JSON
            try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonObject() || jsonElement.isJsonArray()) {
                    DatabaseUtil.addItem(importValue);
                } else {
                    throw new IllegalArgumentException("URL does not return a valid JSON");
                }
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("URL does not return a valid JSON", e);
            }
        } catch (URISyntaxException | MalformedURLException e) {
            ExceptionHandler.handleException(new IllegalArgumentException("Invalid URL: " + importValue));
        } catch (IOException e) {
            ExceptionHandler.handleException(new IllegalArgumentException("Failed to read from URL: " + importValue, e));
        }
    }
}