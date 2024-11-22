package org.fireworkrocket.lookup;

import java.io.File;

public final class Config {
    public static final String NAME = "Lookup";
    public static final double VERSION = 1.2;
    public static final String RELEASE_TYPE = "Preview";


    public static int GetPicNum = 10;
    public static int LoadBatchSize = 10; // 每次加载的图片批次大小
    public static int PauseTransitionMillis = 100; // 加载更多图片的暂停过渡时间（毫秒）
    public static int DebounceTransitionMillis = 500; // 防抖动的暂停过渡时间（毫秒）
    public static int ImageadditionalRows = 3; // 预加载3行

    public static boolean EnableinvertedColor = false; // 启用反色
    public static boolean AUTO_Load_Image = true; // 自动加载图片
    public static boolean STOP_CHANGER_WALLPAPER = true; //停止自动更改壁纸
    public static boolean CheckConnected = true; // 检查网络连接

    public static int ThreadPoolSize = 2; // 图片控制器线程池大小
    public static int PicProcessingSemaphore = 2; // 图片处理器线程池大小

    public static File BGfile = new File("C:\\Users\\User\\Desktop\\th (1).jpg");
    
    
}
