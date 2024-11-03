package org.fireworkrocket.lookup.function.wallpaperchanger;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;

/**
 * 动态壁纸类，用于配置和设置动态壁纸。
 */
public class DynamicWallpaper {

    private static final int GWL_STYLE = -16;
    private static final int WS_CAPTION = 0x00C00000;
    private static final int WS_THICKFRAME = 0x00040000;
    private static final int SWP_SHOWWINDOW = 0x0040;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_NOACTIVATE = 0x0002;
    private static final int RDW_INVALIDATE = 0x0001;
    private static final int RDW_UPDATENOW = 0x0002;

    private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int SCREEN_WIDTH = (int) SCREEN_SIZE.getWidth();
    private static final int SCREEN_HEIGHT = (int) SCREEN_SIZE.getHeight();

    private final AnchorPane root = new AnchorPane();

    /**
     * User32Extended接口，扩展了User32接口。
     */
    public interface User32Extended extends User32 {
        User32Extended INSTANCE = Native.load("user32", User32Extended.class, W32APIOptions.DEFAULT_OPTIONS);

        HWND FindWindowEx(HWND hwndParent, HWND hwndChildAfter, String lpszClass, String lpszWindow); //寻找窗口
        boolean ShowWindow(HWND hWnd, int nCmdShow); //显示窗口
        int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong); //设置窗口属性
        int GetWindowLong(HWND hWnd, int nIndex); //获取窗口属性
        boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags); //设置窗口位置
        boolean RedrawWindow(HWND hWnd, Pointer lprcUpdate, Pointer hrgnUpdate, int flags); //重绘窗口
        boolean IsWindowVisible(HWND hWnd); //窗口是否可见
        boolean GetWindowRect(HWND hWnd, WinDef.RECT rect) ; //获取窗口位置
    }

    /**
     * 配置壁纸。
     */
    private void configureWallpaper() {
        try {
            WinNT.OSVERSIONINFOEX osVersionInfo = new WinNT.OSVERSIONINFOEX();
            Kernel32.INSTANCE.GetVersionEx(osVersionInfo);
            boolean isNewVersion = osVersionInfo.dwMajorVersion.intValue() >= 10 && osVersionInfo.dwBuildNumber.intValue() >= 26100;

            HWND hProgman = User32.INSTANCE.FindWindow("Progman", null); //寻找Progman窗口
            if (hProgman == null) {
                System.err.println("未找到 Progman 窗口。");
                return;
            }

            User32.INSTANCE.SendMessageTimeout(hProgman, 0x52C, null, null, 0, 100, null); //向Progman窗口发送0X52C消息
            Thread.sleep(10);

            HWND hWorkerW = findWorkerW();
            if (hWorkerW != null) {
                configureJavaFxWindow(hWorkerW);
            } else {
                System.err.println("未找到 WorkerW 窗口。");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查找WorkerW窗口。
     *
     * @return WorkerW窗口句柄
     */
    private HWND findWorkerW() {
        HWND[] hWorkerW = new HWND[]{null};
        User32Extended.INSTANCE.EnumWindows((hwnd, arg) -> {
            HWND hDefView = User32Extended.INSTANCE.FindWindowEx(hwnd, null, "SHELLDLL_DefView", null); //寻找SHELLDLL_DefView窗口
            if (hDefView != null) {
                hWorkerW[0] = User32Extended.INSTANCE.FindWindowEx(null, hwnd, "WorkerW", null);
            }
            return true;
        }, Pointer.NULL);

        if (hWorkerW[0] == null) {
            User32Extended.INSTANCE.EnumWindows((hwnd, arg) -> {
                HWND hWorkerWTemp = User32Extended.INSTANCE.FindWindowEx(null, hwnd, "WorkerW", null);
                if (hWorkerWTemp != null) {
                    hWorkerW[0] = hWorkerWTemp;
                }
                return true;
            }, Pointer.NULL);
        }
        return hWorkerW[0];
    }

    /**
     * 配置JavaFX窗口。
     *
     * @param hWorkerW WorkerW窗口句柄
     */
    private void configureJavaFxWindow(HWND hWorkerW) {
        HWND hJavaFX = User32.INSTANCE.FindWindow(null, "动态壁纸");
        if (hJavaFX != null) {
            User32Extended.INSTANCE.ShowWindow(hJavaFX, User32.SW_SHOW);
            User32.INSTANCE.SetParent(hJavaFX, hWorkerW);

            int style = User32Extended.INSTANCE.GetWindowLong(hJavaFX, GWL_STYLE);
            style &= ~WS_CAPTION;
            style &= ~WS_THICKFRAME;
            User32Extended.INSTANCE.SetWindowLong(hJavaFX, GWL_STYLE, style);

            User32Extended.INSTANCE.SetWindowPos(hJavaFX, new HWND(Pointer.createConstant(-1)), 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SWP_SHOWWINDOW);
            User32Extended.INSTANCE.RedrawWindow(hJavaFX, null, null, RDW_INVALIDATE | RDW_UPDATENOW);
            User32Extended.INSTANCE.SetWindowPos(hJavaFX, new HWND(Pointer.createConstant(0)), 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SWP_SHOWWINDOW | SWP_NOZORDER | SWP_NOACTIVATE);

            logWindowInfo(hJavaFX);
        } else {
            System.err.println("未找到 JavaFX 窗口。");
        }
    }

    /**
     * 记录窗口信息。
     *
     * @param hJavaFX JavaFX窗口句柄
     */
    private void logWindowInfo(HWND hJavaFX) {
        boolean isVisible = User32Extended.INSTANCE.IsWindowVisible(hJavaFX);
        System.out.println("JavaFX 窗口可见性: " + isVisible);

        WinDef.RECT rect = new WinDef.RECT();
        User32Extended.INSTANCE.GetWindowRect(hJavaFX, rect);
        System.out.println("JavaFX 窗口位置: " + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom);

        if (rect.left != 0 || rect.top != 0 || rect.right != SCREEN_WIDTH || rect.bottom != SCREEN_HEIGHT) {
            User32Extended.INSTANCE.SetWindowPos(hJavaFX, new HWND(Pointer.createConstant(-1)), 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SWP_SHOWWINDOW);
        }
    }

    /**
     * 设置动态壁纸的方法。
     * <p>
     * 此方法接收一个JavaFX的Parent节点作为参数，并将其设置为动态壁纸。
     * 它首先检查传入的Parent节点是否为空，如果为空则抛出RuntimeException。
     * 然后，它在JavaFX应用线程中创建一个新的Stage，并对其进行配置：
     * <p>
     * - 设置窗口样式为无装饰（UNDECORATED）。
     * <p>
     * - 设置窗口总是置顶（always on top）。
     * <p>
     * - 设置全屏显示（full screen）。
     * <p>
     * - 设置全屏退出提示信息。
     * <p>
     * - 禁止窗口调整大小。
     * <p>
     * - 将传入的Parent节点设置为Scene，并将Scene设置到Stage中。
     * <p>
     * - 显示Stage。
     * <p>
     *
     * 同时，它还为Stage设置了关闭请求的处理逻辑：
     * <p>
     * - 当用户请求关闭窗口时，首先终止Windows资源管理器（explorer.exe）进程。
     * <p>
     * - 等待一秒钟以确保资源管理器进程被终止。
     * <p>
     * - 重新启动资源管理器进程。
     * <p>
     * - 退出JavaFX应用程序。
     * <p>
     * 最后，启动一个新线程来配置壁纸，此配置过程包括查找特定的Windows窗口并将JavaFX窗口嵌入其中。
     * <p>
     * 示例：
     * <pre>
     * {@code
     * DynamicWallpaper dynamicWallpaper = new DynamicWallpaper();
     * Parent parent = new AnchorPane();
     * parent.getChildren().add(new Label("Hello, World!"));
     * dynamicWallpaper.setWallpaper(parent); //parent为JavaFX的Parent节点
     * }
     * </pre>
     *
     * @param parent JavaFX父节点，作为动态壁纸的内容
     * @throws RuntimeException 如果传入的父节点为空
     * @note 请确保在JavaFX应用程序的主线程中调用此方法，此方法在Windows 11 24H2 上未经测试
     */
    public void setWallpaper(Parent parent) throws RuntimeException {
        if (parent == null) {
            throw new RuntimeException("Parent is null");
        }
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("即将就绪");
            stage.setResizable(false);
            stage.setScene(new Scene(parent));
            stage.show();

            stage.setOnCloseRequest(_ -> {
                try {
                    System.out.println("重启资源管理器...");
                    new ProcessBuilder("taskkill", "/f", "/im", "explorer.exe").start().waitFor();
                    Thread.sleep(1000); // Wait for a second to ensure explorer.exe is killed
                    new ProcessBuilder("explorer.exe").start();
                    Platform.runLater(() -> {
                        System.out.println("Exiting JavaFX application...");
                        Platform.exit();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        new Thread(this::configureWallpaper).start();
    }
}