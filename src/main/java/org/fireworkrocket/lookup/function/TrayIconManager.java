package org.fireworkrocket.lookup.function;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.fireworkrocket.lookup.Main;
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;

import java.awt.*;
import java.awt.event.ActionListener;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;

public class TrayIconManager {
    private static TrayIcon trayIcon;
    private static Image trayImage;
    private static PopupMenu trayPopup;
    private static boolean isInitialized = false;

    public static void initializeTrayIcon(Stage stage) throws AWTException {
        if (isInitialized) {
            return;
        }
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
            tray.remove(trayIcon);
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

        isInitialized = true;
    }

    public static void showTrayMessage(String caption, String text, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }
}