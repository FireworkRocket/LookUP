package org.fireworkrocket.lookup.kernel.processor.function;

import org.fireworkrocket.lookup.Config;
import org.fireworkrocket.lookup.kernel.processor.json_read_configuration.JSON_Data_Processor;

import java.util.*;
import java.util.concurrent.*;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleDebug;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.kernel.processor.function.NetworkUtil.isConnected;

public class PicProcessing {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(3);
    public static int picNum = 1;

    public static String[] apiList = org.fireworkrocket.lookup.kernel.processor.DatabaseUtil.getApiList();

    public static long lastCallTime = 0;
    static Semaphore semaphore = new Semaphore(Config.picProcessingSemaphore); // 限制并发请求数量

    private static final Map<String, Integer> apiFailureCount = new ConcurrentHashMap<>();
    private static final Map<String, Long> apiLastFailureTime = new ConcurrentHashMap<>();

    @Deprecated(since = "1.1")
    public static List<String> getPic() {
        if (!isConnected()){
            handleException(new Exception("无网络连接"));
            return Collections.emptyList();
        }
        if (apiList.length == 0) {
            handleException(new Exception("未找到可用的 API"));
            return null;
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

    public static CompletableFuture<String> getPicAtNow() {
        if (!isConnected()) {
            handleException(new Exception("无网络连接"));
            return CompletableFuture.completedFuture(null);
        }
        if (apiList.length == 0) {
            handleException(new Exception("未找到可用的 API"));
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

    private static CompletableFuture<String> getPicUrlAsync(String api) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPicUrl(api);
            } catch (Exception e) {
                apiFailureCount.merge(api, 1, Integer::sum);
                apiLastFailureTime.put(api, System.currentTimeMillis());
                if (apiFailureCount.get(api) >= 3) {
                    handleException(new Exception("API " + api + " 调用失败超过3次，暂时禁用5分钟"));
                }
                throw new RuntimeException("获取图片失败: " + e.getMessage(), e);
            }
        }, forkJoinPool);
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