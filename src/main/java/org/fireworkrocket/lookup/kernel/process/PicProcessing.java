package org.fireworkrocket.lookup.kernel.process;

import org.fireworkrocket.lookup.kernel.config.DatabaseUtil;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.json_configuration.JSON_Data_Processor;

import java.util.*;
import java.util.concurrent.*;

import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleDebug;
import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.kernel.process.net.util.NetworkUtil.isConnected;

/**
 * 图片处理类，用于从 API 获取图片 URL 并进行处理。
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * List<String> urls = PicProcessing.getPic();
 * System.out.println(urls);
 *
 * CompletableFuture<String> urlFuture = PicProcessing.getPicAtNow();
 * urlFuture.thenAccept(url -> System.out.println("获取到的图片 URL: " + url));
 * }</pre>
 */
public class PicProcessing {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(3);
    public static int picNum = 1;

    public static String[] apiList = DatabaseUtil.getApiList();

    public static long lastCallTime = 0;
    static Semaphore semaphore = new Semaphore(DefaultConfig.picProcessingSemaphore); // 限制并发请求数量

    private static final Map<String, Integer> apiFailureCount = new ConcurrentHashMap<>();
    private static final Map<String, Long> apiLastFailureTime = new ConcurrentHashMap<>();
    private static final int MAX_CALLS = 10; // 最大调用次数
    private static final long MIN_COOLDOWN = 500; // 最小冷却时间，单位为毫秒
    private static final long MAX_COOLDOWN = 5 * 1000; // 最大冷却时间，单位为毫秒

    private static int callCount = 0;
    private static long cooldownTime = MIN_COOLDOWN;

    /**
     * 获取图片 URL 列表。
     *
     * @return 图片 URL 列表
     */
    @Deprecated(since = "1.1")
    public static List<String> getPic() {
        if (!verificationAPI()){
            return Collections.emptyList();
        }

        if (checkCallFrequency()) return Collections.emptyList();

        List<CompletableFuture<String>> futures = new ArrayList<>();
        Random random = new Random(); // 在循环外部创建 Random 实例
        for (int i = 0; i < picNum; i++) {
            int apiIndex = random.nextInt(apiList.length);
            futures.add(getPicUrlAsync(apiList[apiIndex]));
        }

        List<String> urls = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                String url = future.get();
                if (url != null && !url.isEmpty()) {
                    urls.add(url);
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
        return urls;
    }

    /**
     * 立即获取图片 URL。
     *
     * @return 图片 URL 的 CompletableFuture
     */
    public static CompletableFuture<String> getPicAtNow() {
        if (!verificationAPI()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        Random random = new Random();
        int requestCount = 0;

        for (int i = 0; i < picNum; i++) {
            if (requestCount >= picNum) break;
            int apiIndex = random.nextInt(apiList.length);
            String api = apiList[apiIndex];

            if (apiFailureCount.getOrDefault(api, 0) >= 3 &&
                    System.currentTimeMillis() - apiLastFailureTime.getOrDefault(api, 0L) < 5 * 60 * 1000) {
                continue;
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    return getPicUrl(api);
                } catch (Exception e) {
                    apiFailureCount.merge(api, 1, Integer::sum);
                    apiLastFailureTime.put(api, System.currentTimeMillis());
                    if (apiFailureCount.get(api) >= 3) {
                        handleException(new Exception("API " + api + " 调用失败超过3次，暂时禁用5分钟"));
                    }
                    throw new RuntimeException("获取图片失败: " + e.getMessage(), e);
                } finally {
                    semaphore.release();
                }
            }, forkJoinPool);

            futures.add(future);
            requestCount++;
        }

        return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(result -> (String) result)
                .exceptionally(e -> {
                    handleException(e);
                    return null;
                });
    }

    /**
     * 验证 API 是否可用。
     *
     * @return 如果 API 可用则返回 true，否则返回 false
     */
    private static boolean verificationAPI() {
        if (!isConnected()) {
            handleException(new Exception("无网络连接"));
            return false;
        }
        if (apiList.length == 0) {
            handleException(new Exception("未找到可用的 API"));
            return false;
        }

        if (getDisabledApis().size() == apiList.length) {
            handleException(new Exception("所有 API 都被禁用"));
            return false;
        }
        return true;
    }

    /**
     * 异步获取图片 URL。
     *
     * @param api API 地址
     * @return 图片 URL 的 CompletableFuture
     */
    private static CompletableFuture<String> getPicUrlAsync(String api) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPicUrl(api);
            } catch (Exception e) {
                apiFailureCount.merge(api, 1, Integer::sum);
                apiLastFailureTime.put(api, System.currentTimeMillis());
                if (apiFailureCount.get(api) >= 3) {
                    handleException(new Exception("API " + api + " 调用失败超过3次，暂���禁用5分钟"));
                }
                throw new RuntimeException("获取图片失败: " + e.getMessage(), e);
            }
        }, forkJoinPool);
    }

    /**
     * 检查调用频率。
     *
     * @return 如果调用频率过高则返回 true，否则返回 false
     */
    private static boolean checkCallFrequency() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime < cooldownTime) {
            handleException(new Exception("调用 GetPic() 不能超过每 " + (cooldownTime / 1000) + " 秒一次"));
            return true;
        } else {
            callCount++;
            if (callCount > MAX_CALLS) {
                cooldownTime = Math.min(cooldownTime * 2, MAX_COOLDOWN); // 动态调整冷却时间
                callCount = 0; // 重置计数器
            } else {
                cooldownTime = MIN_COOLDOWN; // 重置冷却时间
            }
            handleDebug("设置 " + (cooldownTime / 1000) + " 秒冷却期...");
            lastCallTime = currentTime;
            return false;
        }
    }

    /**
     * 获取图片 URL。
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
     * 从结果映射中提取 URL。
     *
     * @param resultMap 结果映射
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
     * 关闭图片处理器。
     */
    public static void picProcessingShutdown() {
        executorService.shutdown();
        forkJoinPool.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!forkJoinPool.awaitTermination(60, TimeUnit.SECONDS)) {
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            forkJoinPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查 API 可用性。
     */
    public static void checkApiAvailability() {
        executorService.scheduleAtFixedRate(() -> {
            for (String api : apiList) {
                if (apiFailureCount.getOrDefault(api, 0) >= 3) {
                    try {
                        getPicUrl(api);
                        apiFailureCount.put(api, 0);
                        handleDebug("API " + api + " 可用，重置失败计数器");
                    } catch (Exception e) {
                        handleDebug("API " + api + " 仍不可用");
                    }
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    /**
     * 获取被禁用的 API 列表。
     *
     * @return 被禁用的 API 列表
     */
    public static List<String> getDisabledApis() {
        List<String> disabledApis = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (String api : apiList) {
            if (apiFailureCount.getOrDefault(api, 0) >= 3 &&
                    currentTime - apiLastFailureTime.getOrDefault(api, 0L) < 5 * 60 * 1000) {
                disabledApis.add(api);
            }
        }
        return disabledApis;
    }
}