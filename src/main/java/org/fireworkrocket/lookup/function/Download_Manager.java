package org.fireworkrocket.lookup.function;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.*;

/**
 * Download_Manager 类，用于管理文件下载。
 */
public class Download_Manager {
    public static String filePath = ""; // 文件路径
    public static String savePath = ""; // 保存路径
    private static final int THREAD_COUNT = 8; // 下载线程数
    private static final AtomicLong downloadedBytes = new AtomicLong(0);
    private static final AtomicLong startTime = new AtomicLong(0);
    private static volatile boolean isPaused = false;

    /**
     * 显示下载进度。
     *
     * @param totalSize 文件总大小
     */
    private static void showProgress(int totalSize) {
        long elapsedTime = System.currentTimeMillis() - startTime.get(); // 计算已经过去的时间
        double downloadSpeed = (downloadedBytes.get() / (1024.0 * 1024.0)) / (elapsedTime / 1000.0); // MB/s
        double progress = (downloadedBytes.get() / (double) totalSize) * 100; // 百分比
        int progressBarLength = 50; // 进度条长度
        int filledLength = (int) (progressBarLength * progress / 100);

        StringBuilder progressBar = new StringBuilder("|");
        for (int i = 0; i < progressBarLength; i++) {
            progressBar.append(i < filledLength ? "█" : "-");
        }
        progressBar.append("|");

        String end = downloadedBytes.get() >= totalSize ? "\n" : ""; // 下载完成后换行
        System.out.printf("\rDownloaded: %s  %.2f MB/s%s", progressBar, downloadSpeed, end); // 使用 printf 方法格式化输出
    }

    /**
     * 通过指定的 URL 下载文件并将其保存到给定路径。
     *
     * @param urlStr 要下载的文件的 URL
     * @param savePath 文件保存路径
     * @param Return 是否返回文件路径
     * @return 如果 Return 为 true，则返回文件路径，否则返回 null
     */
    public static String downLoadByUrl(String urlStr, String savePath, boolean Return) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT); // 使用固定大小的线程池
        AtomicInteger completedThreads = new AtomicInteger(0);
        AtomicBoolean retry = new AtomicBoolean(false);
        try {
            handleDebug("Downloading file from: " + urlStr);
            String fileName = getFileName(urlStr);
            String redirectedUrl = getRedirectedUrl(urlStr);
            URL url = new URL(redirectedUrl);
            int totalSize = getContentLength(url);

            File saveDir = new File(savePath);
            if (!saveDir.exists()) {
                saveDir.mkdir(); // 创建目录
                handleDebug("Directory created: " + saveDir);
            }
            File file = new File(saveDir, fileName);

            int partSize = totalSize / THREAD_COUNT;
            startTime.set(System.currentTimeMillis());
            for (int i = 0; i < THREAD_COUNT; i++) { // 使用循环启动多个下载任务
                int start = i * partSize; // 计���每个线程的开始和结束位置
                int end = (i == THREAD_COUNT - 1) ? totalSize : (start + partSize - 1); // 最后一个线程下载剩余部分
                executor.execute(new DownloadTask(url, file, start, end, completedThreads, THREAD_COUNT, retry)); // 使用 execute 方法提交任务
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                showProgress(totalSize);
                Thread.sleep(16); // 将休眠时间减少到16毫秒
            }
            if (retry.get() || downloadedBytes.get() != totalSize) {
                handleWarning("Retrying download with single thread due to size mismatch.");
                downloadedBytes.set(0);
                startTime.set(System.currentTimeMillis());
                new DownloadTask(url, file, 0, totalSize - 1, completedThreads, 1, retry).run();
            }
            if (downloadedBytes.get() == totalSize) {
                System.out.print("\r\u001B[32m *Download completed \u001B[0m\n");
                handleInfo("File Download Success: " + file.getAbsolutePath());
            } else {
                System.err.println("*Download failed \n");
            }

            if (Return) {
                return file.getAbsolutePath();
            }
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * 获取文件内容长度。
     *
     * @param url 文件的 URL
     * @return 文件的总大小
     * @throws IOException 如果获取内容长度失败
     */
    private static int getContentLength(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.5410.0 Safari/537.36");
        int totalSize = conn.getContentLength();
        conn.disconnect();
        return totalSize;
    }

    /**
     * 下载任务类。
     */
    private static class DownloadTask implements Runnable {
        private final URL url;
        private final File file;
        private final int start;
        private final int end;
        private final AtomicInteger completedThreads;
        private final int totalThreads;
        private final AtomicBoolean retry;

        /**
         * 构造函数。
         *
         * @param url 文件的 URL
         * @param file 保存的文件
         * @param start 下载的开始位置
         * @param end 下载的结束位置
         * @param completedThreads 已完成的线程数
         * @param totalThreads 总线程数
         * @param retry 是否重试
         */
        public DownloadTask(URL url, File file, int start, int end, AtomicInteger completedThreads, int totalThreads, AtomicBoolean retry) {
            this.url = url;
            this.file = file;
            this.start = start;
            this.end = end;
            this.completedThreads = completedThreads;
            this.totalThreads = totalThreads;
            this.retry = retry;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                conn = (HttpURLConnection) url.openConnection(); // 打开连接以设置属性
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                conn.connect();
                int responseCode = conn.getResponseCode(); // 获取响应代码

                if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                    handleWarning("Server did not return a valid response. Response Code: " + responseCode);
                    retry.set(true);
                    conn.disconnect();
                    return;
                }
                int contentLength = conn.getContentLength();
                if (start >= contentLength) { // 检查开始位置是否超出内容长度
                    handleWarning("Start position is beyond content length");
                    completedThreads.incrementAndGet();
                    conn.disconnect();
                    return;
                }
                int adjustedEnd = Math.min(end, contentLength - 1);
                conn.disconnect(); // 断开连接以设置新属性
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + adjustedEnd); // 设置新范围
                responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) { // 检查新响应代码
                    handleWarning("Server did not return a valid response after reconnect. Response Code: " + responseCode);
                    retry.set(true);
                    conn.disconnect();
                    return;
                }
                InputStream inputStream = conn.getInputStream();
                raf.seek(start);
                byte[] buffer = new byte[8 * 1024]; // 增加缓冲区大小到8 KB
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    synchronized (Download_Manager.class) {
                        while (isPaused) {
                            Download_Manager.class.wait(); // 暂停下载
                        }
                    }
                    raf.write(buffer, 0, len);
                    downloadedBytes.addAndGet(len); // 在这里更新 downloadedBytes
                }
                inputStream.close();
                conn.disconnect();
            } catch (IOException | InterruptedException e) {
                handleException("Download failed", e);
                retry.set(true);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                completedThreads.incrementAndGet();
            }
        }

        /**
         * 获取已完成的线程数。
         *
         * @return 已完成的线程数
         */
        public AtomicInteger getCompletedThreads() {
            return completedThreads;
        }

        /**
         * 获取总线程数。
         *
         * @return 总线程数
         */
        public int getTotalThreads() {
            return totalThreads;
        }
    }

    /**
     * 获取文件名。
     *
     * @param srcRealPath 文件的实际路径
     * @return 文件名
     */
    private static String getFileName(String srcRealPath) {
        String fileName = StringUtils.substringAfterLast(srcRealPath, "/");
        int queryIndex = fileName.indexOf("?");
        if (queryIndex != -1) {
            fileName = fileName.substring(0, queryIndex);
        }
        return fileName;
    }

    /**
     * 获取重定向后的 URL。
     *
     * @param urlStr 原始 URL
     * @return 重定向后的 URL
     */
    private static String getRedirectedUrl(String urlStr) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
            connection.setInstanceFollowRedirects(false);
            String redirectedUrl = connection.getHeaderField("Location"); // 获取重定向 URL
            connection.disconnect();
            if (redirectedUrl != null && !redirectedUrl.isEmpty()) { // 检查重定向 URL 是否为空
                if (!redirectedUrl.startsWith("http://") && !redirectedUrl.startsWith("https://")) {
                    redirectedUrl = "http://" + redirectedUrl; // 添加协议
                }
                return redirectedUrl;
            } else {
                if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                    urlStr = "http://" + urlStr;
                }
                return urlStr;
            }
        } catch (IOException e) {
            handleException("Failed to get redirected URL", e);
        }
        return null;
    }

    /**
     * 暂停下载。
     */
    public static synchronized void pauseDownload() {
        isPaused = true;
    }

    /**
     * 恢复下载。
     */
    public static synchronized void resumeDownload() {
        isPaused = false;
        Download_Manager.class.notifyAll(); // 通知所有等待线程
    }
}