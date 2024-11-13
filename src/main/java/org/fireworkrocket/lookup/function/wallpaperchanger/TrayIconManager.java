package org.fireworkrocket.lookup.function.wallpaperchanger;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.fireworkrocket.lookup.Main;

import java.awt.*;
import java.awt.event.ActionListener;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;

public class TrayIconManager {
    private static TrayIcon trayIcon;
    private static Image trayImage;
    private static PopupMenu trayPopup;

    public static void initializeTrayIcon(Stage stage) throws AWTException {
        if (!SystemTray.isSupported()) {
            handleException(new Exception("System tray not supported!"));
            Platform.exit();
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        if (trayImage == null) {
            trayImage = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("icon.png"));
        }

        ActionListener showListener = _ -> Platform.runLater(stage::show);
        ActionListener getNewPicItemListener = _ -> WallpaperChanger.getTodayWallpaper();
        ActionListener exitListener = _ -> {
            Main.getService().shutdown();
            System.exit(0);
        };

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

        trayIcon = new TrayIcon(trayImage, "Look UP!", trayPopup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(showListener);
        tray.add(trayIcon);
    }

    public static TrayIcon getTrayIcon() {
        return trayIcon;
    }
}