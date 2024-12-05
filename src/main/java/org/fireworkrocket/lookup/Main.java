package org.fireworkrocket.lookup;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.commons.cli.*;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.config.UserConfigImpl;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;
import org.fireworkrocket.lookup.ui.exception.DialogUtil;
import org.fireworkrocket.lookup.ui.wallpaper.ListeningWallpaper;
import org.fireworkrocket.lookup.ui.TrayIconManager;
import org.fireworkrocket.lookup.ui.wallpaper.WallpaperChanger;
import org.fireworkrocket.lookup.kernel.config.DatabaseUtil;

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
    private static Stage bootStage = new Stage();
    private static Label bootLabel = new Label();
    public static final String logFilename = "Logs/Debug-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".log";
    static long startTime = 0;

    public static void main(String[] args) throws IOException {
        startTime = System.currentTimeMillis();
        handleCommandLineArgs(args);
        initializeBootStage();

        initializeLogging();
        suspendProcesses();
        new UserConfigImpl();
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        if (!SystemTray.isSupported()) {
            handleException(new Exception("System tray not supported!"));
            Platform.exit();
            return;
        }

        updateBootLabel("验证网络连接...");
        new Thread(() -> stage.setTitle(isConnected() ? "Look UP!" : "Look UP! - 离线模式")).start();

        if (!stop_Changer_Wallpaper) {
            scheduleWallpaperChanger();
        }

        scheduleGarbageCollection();
        initializeTrayIcon(stage);
        configureStageCloseRequest(stage);
        loadFXMLAndShowStage(stage);
    }

    private static void handleCommandLineArgs(String[] args) {
        if (args.length != 0) {
            Options options = new Options();
            options.addOption(new Option("offline", "run in offline mode"));
            options.addOption(new Option("import", true, "import data"));

            CommandLineParser parser = new DefaultParser();
            try {
                CommandLine cmd = parser.parse(options, args);
                if (cmd.hasOption("offline")) {
                    DefaultConfig.checkConnected = false;
                }
                if (cmd.hasOption("import")) {
                    processImportValue(cmd.getOptionValue("import"));
                }
            } catch (ParseException e) {
                ExceptionHandler.handleException(new IllegalArgumentException("Failed to parse command line arguments", e));
            }
            System.exit(0);
        }
    }

    private static void initializeBootStage() {
        Platform.runLater(() -> {
            AnchorPane pane = new AnchorPane();

            ImageView imageView = new ImageView(new Image(String.valueOf(Main.class.getResource("BootUp.png"))));
            imageView.setSmooth(true);
            imageView.setPreserveRatio(false); // 保持图片比例
            imageView.setFitWidth(638); // 设置图片宽度
            imageView.setFitHeight(319); // 设置图片高度
            AnchorPane.setTopAnchor(imageView, 0.0);
            AnchorPane.setLeftAnchor(imageView, 0.0);
            AnchorPane.setRightAnchor(imageView, 0.0);

            bootLabel.setStyle("-fx-text-fill: rgb(255,255,255);");
            bootLabel.setWrapText(true); // 确保文字能够换行显示
            bootLabel.setMaxWidth(580); // 设置最大宽度，避免文字超出边界
            AnchorPane.setBottomAnchor(bootLabel, 10.0); // 设置文字位置
            AnchorPane.setLeftAnchor(bootLabel, 12.0);
            AnchorPane.setRightAnchor(bootLabel, 10.0);

            pane.getChildren().addAll(imageView, bootLabel);

            bootStage.setWidth(imageView.getFitWidth());
            bootStage.setHeight(imageView.getFitHeight());
            bootStage.initStyle(StageStyle.UNDECORATED);
            bootStage.setTitle("Look Up");
            bootStage.setAlwaysOnTop(true);
            bootStage.setScene(new Scene(pane));
            bootStage.show();
        });
    }

    private static void initializeLogging() {
        updateBootLabel("请稍后：log4j2...");
        System.setProperty("logFilename", logFilename);
        System.setProperty("log4j.configurationFile", Objects.requireNonNull(Main.class.getResource("log4j2.xml")).toString());
    }

    private static void suspendProcesses() {
        updateBootLabel("等待：setProcessSuspendable...");
        int[] processes = listProcesses();
        for (int pid : processes) {
            setProcessSuspendable(pid);
        }
    }

    private static void scheduleWallpaperChanger() {
        wallpaperChangerFuture = getService().scheduleAtFixedRate(WallpaperChanger::getTodayWallpaper, 0, 1, TimeUnit.DAYS);
        new Thread(() -> new ListeningWallpaper().startListening()).start();
    }

    private static void scheduleGarbageCollection() {
        getService().scheduleAtFixedRate(System::gc, 0, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(getService()::shutdown));
    }

    private static void initializeTrayIcon(Stage stage) {
        updateBootLabel("AWT> 请稍后: TrayIcon...");
        SwingUtilities.invokeLater(() -> {
            try {
                TrayIconManager.initializeTrayIcon(stage);
            } catch (AWTException e) {
                handleException(e);
            }
        });
    }

    private static void configureStageCloseRequest(Stage stage) {
        updateBootLabel("请稍后：setOnCloseRequest...");
        stage.setOnCloseRequest(event -> {
            Map<String, Runnable> buttons = new HashMap<>();
            buttons.put("是", () -> {
                event.consume();
                stage.hide();
            });
            buttons.put("否", () -> new Thread(() -> {
                getService().shutdown();
                Platform.runLater(() -> System.exit(0));
            }).start());
            DialogUtil.showDialog(DialogType.INFO, "关闭", "最小化到托盘？", buttons);
        });
    }

    private static void loadFXMLAndShowStage(Stage stage) {
        updateBootLabel("JavaFX> 请稍后: FXML...");
        Platform.runLater(() -> {
            try {
                Parent root = FXMLLoader.load(Objects.requireNonNull(Main.class.getResource("Home.fxml")));
                Scene scene = new Scene(root);
                applyMaterialFXThemes();
                stage.setScene(scene);
                stage.show();
                closeBootStage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void applyMaterialFXThemes() {
        updateBootLabel("JavaFX> 请稍后: Materialfx...");
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();
    }

    private static void closeBootStage() {
        long endTime = System.currentTimeMillis();
        long startupTime = endTime - startTime;
        updateBootLabel("就绪！（" + startupTime / 1000 + "s）");

        PauseTransition pause = new PauseTransition(Duration.seconds(0.43));
        pause.setOnFinished(event -> {
            bootStage.close();
            bootLabel = null;
            bootStage = null;
        });
        pause.play();
    }

    private static void updateBootLabel(String text) {
        Platform.runLater(() -> bootLabel.setText(text));
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
            URL url = new URI(importValue).toURL();
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