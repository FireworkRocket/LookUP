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
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
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

/**
 * Main 类，应用程序的入口点。
 */
public class Main extends Application {
    private ScheduledExecutorService service;
    private Image trayImage;
    private PopupMenu trayPopup;

    public static String logFilename = "Logs/Debug-" + System.currentTimeMillis() + ".log";

    /**
     * 应用程序的主方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置全局异常处理器
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
        launch(args);
    }

    /**
     * 启动 JavaFX 应用程序。
     *
     * @param stage 主舞台
     * @throws IOException 如果加载 FXML 文件失败
     */
    @Override
    public void start(Stage stage) throws IOException {
        if (!SystemTray.isSupported()) {
            handleException(new Exception("System tray not supported!"));
            Platform.exit();
            return;
        }

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Home.fxml")));

        Scene scene = new Scene(root);
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();
        stage.setTitle("Look UP!");
        stage.getIcons().add(new javafx.scene.image.Image(Objects.requireNonNull(Main.class.getResource("icon.png")).toString()));
        stage.setScene(scene);
        stage.show();

        // 每天更换壁纸
        boolean stopChanger = false;
        if (!stopChanger) {
            getService().scheduleAtFixedRate(WallpaperChanger::getTodayWallpaper, 0, 1, TimeUnit.DAYS);
        }

        // 每10秒自动回收垃圾
        getService().scheduleAtFixedRate(System::gc, 0, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(getService()::shutdown));

        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(() -> {
            try {
                addAppToTray(stage);
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
            DialogUtil.showDialog(DialogType.ERROR, "关闭", "最小化到托盘？", buttons);
        });
    }

    /**
     * 将应用程序添加到系统托盘。
     *
     * @param stage 主舞台
     * @throws AWTException 如果系统托盘不支持
     */
    private void addAppToTray(Stage stage) throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();
        if (trayImage == null) {
            trayImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png"));
        }

        ActionListener showListener = _ -> Platform.runLater(stage::show); // 显示
        ActionListener getNewPicItemListener = _ -> WallpaperChanger.getTodayWallpaper(); // 获取新图片
        ActionListener exitListener = _ -> {
            getService().shutdown();
            System.exit(0);
        }; // 退出

        if (trayPopup == null) {
            trayPopup = new PopupMenu();
            MenuItem showItem = new MenuItem("Show");
            showItem.addActionListener(showListener);
            trayPopup.add(showItem);

            MenuItem getNewPicItem = new MenuItem("Get A New Image");
            getNewPicItem.addActionListener(getNewPicItemListener);
            trayPopup.add(getNewPicItem);

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(exitListener);
            trayPopup.add(exitItem);
        }

        TrayIcon trayIcon = new TrayIcon(trayImage, "Look UP!", trayPopup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(showListener);
        tray.add(trayIcon);
    }

    private ScheduledExecutorService getService() {
        if (service == null) {
            service = Executors.newScheduledThreadPool(1);
        }
        return service;
    }
}