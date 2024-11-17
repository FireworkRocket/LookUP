package org.fireworkrocket.lookup.function.wallpaperchanger;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.platform.win32.Guid.GUID;
import org.fireworkrocket.lookup.exception.ExceptionHandler;
import org.fireworkrocket.lookup.function.TrayIconManager;
import static org.fireworkrocket.lookup.Config.STOP_CHANGER_WALLPAPER;
import static org.fireworkrocket.lookup.Main.cancelWallpaperChangerTask;
import static org.fireworkrocket.lookup.Main.getService;

import java.awt.*;

public class ListeningWallpaper {
    public interface MyUser32 extends User32 {
        MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

        void RegisterPowerSettingNotification(HWND hWnd, GUID PowerSettingGuid, int Flags);
    }

    public static final int WM_SETTINGCHANGE = 0x001A;
    private static boolean isAppChangingWallpaper = false;

    public static void setAppChangingWallpaper(boolean isAppChanging) {
        isAppChangingWallpaper = isAppChanging;
    }

    public void startListening() {
        final WinUser.WNDCLASSEX wClass = new WinUser.WNDCLASSEX();
        final String windowClass = "WallpaperChangeListener";
        wClass.lpfnWndProc = (WinUser.WindowProc) (hWnd, uMsg, wParam, lParam) -> {
            if (uMsg == WM_SETTINGCHANGE) {
                if (!isAppChangingWallpaper) {
                    ExceptionHandler.handleInfo("Wallpaper changed by user!");
                    TrayIconManager.showTrayMessage("壁纸更新", "壁纸已由您/其他程序更新，壁纸刷新已暂停", TrayIcon.MessageType.INFO);
                }
                isAppChangingWallpaper = false;
                STOP_CHANGER_WALLPAPER = true;
                cancelWallpaperChangerTask();
            }
            return User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
        };
        wClass.hInstance = Kernel32.INSTANCE.GetModuleHandle(null);
        wClass.lpszClassName = windowClass;

        User32.INSTANCE.RegisterClassEx(wClass);

        WinDef.HWND hWnd = User32.INSTANCE.CreateWindowEx(
                0, windowClass, "Wallpaper Change Listener", 0,
                0, 0, 0, 0, null, null, wClass.hInstance, null);

        MyUser32.INSTANCE.RegisterPowerSettingNotification(hWnd, new GUID("{7516b95f-f776-4464-8c53-06167f40cc99}"), 0);

        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.GetMessage(msg, hWnd, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }
    }
}