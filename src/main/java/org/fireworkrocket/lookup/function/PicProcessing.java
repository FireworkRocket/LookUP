package org.fireworkrocket.lookup.function;

import org.fireworkrocket.lookup.processor.DEFAULT_API_CONFIG;
import org.fireworkrocket.lookup.processor.JSON_Data_Processor;
import org.fireworkrocket.lookup.processor.Trust_All_Certificates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleDebug;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;

/**
 * PicProcessing 类，用于处理图片。
 *
 * @author FireworkRocket
 * @version 0.6_Build_20231013
 * @date 2023-10-13
 */
public class PicProcessing {

    // 线程池
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(3);

    // API 列表
    public static String[] apiList = {
            DEFAULT_API_CONFIG.JitsuApi,
            DEFAULT_API_CONFIG.MIAOMC_API,
    };

    // 上次调用的时间
    public static long lastCallTime = 0;

    /**
     * 获取图片 URL 列表
     *
     * @return 图片 URL 列表
     */
    @Deprecated(since = "1.1")
    public static List<String> getPic() {
        if (checkCallFrequency()) return null;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ConcurrentHashMap.KeySetView<String, Boolean> urlSet = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < DEFAULT_API_CONFIG.picNum; i++) {
            int apiIndex = new Random().nextInt(apiList.length);
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return getPicUrl(apiList[apiIndex]);
                } catch (Exception e) {
                    throw new RuntimeException("获取图片失败: " + e.getMessage(), e);
                }
            }, forkJoinPool).thenAccept(urlSet::add);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new ArrayList<>(urlSet);
    }

    public static String getPicAtNow() throws Exception {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < DEFAULT_API_CONFIG.picNum; i++) {
            int apiIndex = new Random().nextInt(apiList.length);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return getPicUrl(apiList[apiIndex]);
                } catch (Exception e) {
                    throw new RuntimeException("获取图片失败: " + e.getMessage(), e);
                }
            }, forkJoinPool);
            futures.add(future);
        }

        for (CompletableFuture<String> future : futures) {
            try {
                String url = future.get();
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            } catch (Exception e) {
                handleException(e);
            }
        }

        throw new Exception("未能获取到有效的图片 URL");
    }

    private static boolean checkCallFrequency() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime < 5 * 1000) {
            handleException(new Exception("调用 GetPic() 不能超过每 5 秒一次"));
            return true;
        } else {
            handleDebug("设置 5 秒冷却期...");
            lastCallTime = currentTime;
            return false;
        }
    }

    /**
     * 获取图片 URL
     *
     * @param api API 地址
     * @return 图片 URL
     */
    private static String getPicUrl(String api) {
        int totalRetryCount = 0;
        while (totalRetryCount <= 3) {
            try {
                Map<String, Object> resultMap = JSON_Data_Processor.getUrl(api);
                handleDebug("API: " + api + " API 响应: " + resultMap);

                String url = extractUrl(resultMap);
                if (url != null && !url.isEmpty()) {
                    return url;
                } else {
                    throw new Exception("URL 为空或无效");
                }
            } catch (Exception e) {
                handleException(e);
                if (++totalRetryCount > 3) {
                    throw new RuntimeException("重试 3 次后获取图片 URL 失败: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * 提取 URL
     *
     * @param resultMap API 响应结果
     * @return 提取的 URL
     */
    private static String extractUrl(Map<String, Object> resultMap) {
        if (resultMap.containsKey("URL")) {
            return (String) resultMap.get("URL");
        } else {
            for (String key : resultMap.keySet()) {
                if (key.startsWith("$Data")) {
                    Map<String, Object> dataMap = (Map<String, Object>) resultMap.get(key);
                    return (String) dataMap.get("URL");
                }
            }
        }
        return null;
    }

    /**
     * 下载图片
     *
     * @param imageUrl 图片 URL
     * @return 下载的图片路径
     * @throws IOException 如果下载失败
     */
    static Path downloadImage(String imageUrl) throws IOException {
        String tempDirPath = System.getProperty("java.io.tmpdir");
        Path tempDir = Paths.get(tempDirPath, "Look_UP", "ImageTemp");
        Files.createDirectories(tempDir);
        Path outputPath = Files.createTempDirectory(tempDir, "Image");
        Download_Manager.filePath = imageUrl;
        Download_Manager.savePath = outputPath.toString();
        try {
            Trust_All_Certificates.trustAllHttpsCertificates();
        } catch (Exception e) {
            throw new RuntimeException("信任所有证书失败", e);
        }
        outputPath = Path.of(Objects.requireNonNull(Download_Manager.downLoadByUrl(Download_Manager.filePath, Download_Manager.savePath, true)));
        System.out.println("图片下载到: " + outputPath);
        return outputPath;
    }

    /**
     * 开始下载图片
     */
    public static void startDwnPic() {
        // 实现下载逻辑
    }

    /**
     * 关闭线程池
     */
    public static void picProcessingShutdown() {
        executorService.shutdown();
        forkJoinPool.shutdown();
    }
}