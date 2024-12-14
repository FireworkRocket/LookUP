package org.fireworkrocket.lookup.kernel.config;

import java.io.File;

public class DefaultConfig {
    public static int getPicNum = 10;
    public static int loadBatchSize = 10; // 每次加载的图片批次大小
    public static int pauseTransitionMillis = 100; // 加载更多图片的暂停过渡时间（毫秒）
    public static int debounceTransitionMillis = 500; // 防抖动的暂停过渡时间（毫秒）
    public static int imageadditionalRows = 3; // 预加载3行

    public static boolean enableinvertedColor = false; // 启用主页面时间反色
    public static boolean auto_Load_Image = true; // 自动加载图片
    public static boolean stop_Changer_Wallpaper = true; //停止自动更改壁纸
    public static boolean checkConnected = true; // 检查网络连接

    public static int threadPoolSize = 2; // 图片控制器线程池大小
    public static int picProcessingSemaphore = 2; // 图片处理器线程池大小

    public static File backGroundfile = new File("");
    public static File configHome = new File(System.getenv("APPDATA") + "/LookUp/Config");
    public static File tempDownloadPath = new File(System.getenv("APPDATA") + "/LookUp/TempDownload");
    public static String HttpConnectionUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.58 Safari/537.36";
}
