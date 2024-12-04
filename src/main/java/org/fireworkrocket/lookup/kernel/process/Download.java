package org.fireworkrocket.lookup.kernel.process;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.*;

/**
 * 文件下载类，支持多线程下载和断点续传。
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * String url = "http://example.com/file.zip";
 * String savePath = "/path/to/save";
 * String filePath = Download.downLoadByUrl(url, savePath, true);
 * System.out.println("文件下载路径: " + filePath);
 * }</pre>
 */
public class Download {
    public static String filePath = "";
    public static String savePath = "";
    private static final int THREAD_COUNT = 8;
    private static final AtomicLong downloadedBytes = new AtomicLong(0);
    private static final AtomicLong startTime = new AtomicLong(0);
    private static volatile boolean isPaused = false;

    /**
     * 显示下载进度。
     *
     * @param totalSize 文件总大小
     */
    private static void showProgress(int totalSize) {
        long elapsedTime = System.currentTimeMillis() - startTime.get();
        double downloadSpeed = (downloadedBytes.get() / (1024.0 * 1024.0)) / (elapsedTime / 1000.0);
        double progress = (downloadedBytes.get() / (double) totalSize) * 100;
        int progressBarLength = 50;
        int filledLength = (int) (progressBarLength * progress / 100);

        StringBuilder progressBar = new StringBuilder("|");
        for (int i = 0; i < progressBarLength; i++) {
            progressBar.append(i < filledLength ? "█" : "-");
        }
        progressBar.append("|");

        String end = downloadedBytes.get() >= totalSize ? "\n" : "";
        System.out.printf("\rDownloaded: %s  %.2f MB/s%s", progressBar, downloadSpeed, end);
    }

    /**
     * 根据 URL 下载文件。
     *
     * @param urlStr 文件 URL
     * @param savePath 保存路径
     * @param Return 是否返回文件路径
     * @return 下载的文件路径
     */
    public static String downLoadByUrl(String urlStr, String savePath, boolean Return) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
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
                saveDir.mkdirs();
                handleDebug("Directory created: " + saveDir);
            }
            File file = new File(saveDir, fileName);

            int partSize = totalSize / THREAD_COUNT;
            startTime.set(System.currentTimeMillis());
            for (int i = 0; i < THREAD_COUNT; i++) {
                int start = i * partSize;
                int end = (i == THREAD_COUNT - 1) ? totalSize : (start + partSize - 1);
                executor.execute(new DownloadTask(url, file, start, end, completedThreads, THREAD_COUNT, retry));
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                showProgress(totalSize);
                Thread.sleep(1000);
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
     * 获取文件的总大小。
     *
     * @param url 文件 URL
     * @return 文件总大小
     * @throws IOException 如果获取文件大小失败
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
     * 下载任务类，支持多线程下载。
     */
    private static class DownloadTask implements Runnable {
        private final URL url;
        private final File file;
        private final int start;
        private final int end;
        private final AtomicInteger completedThreads;
        private final int totalThreads;
        private final AtomicBoolean retry;

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
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                conn.connect();
                int responseCode = conn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                    handleWarning("Server did not return a valid response. Response Code: " + responseCode);
                    retry.set(true);
                    conn.disconnect();
                    throw new IOException("Server did not return a valid response. Response Code: " + responseCode);
                }
                InputStream inputStream = conn.getInputStream();
                raf.seek(start);
                byte[] buffer = new byte[4 * 1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    synchronized (Download.class) {
                        while (isPaused) {
                            Download.class.wait();
                        }
                    }
                    raf.write(buffer, 0, len);
                    downloadedBytes.addAndGet(len);
                    showProgress(end - start + 1); // 更新进度条
                }
                inputStream.close();
                conn.disconnect();
            } catch (IOException | InterruptedException e) {
                retry.set(true);
                throw new RuntimeException("Download failed", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                completedThreads.incrementAndGet();
            }
        }

        public AtomicInteger getCompletedThreads() {
            return completedThreads;
        }

        public int getTotalThreads() {
            return totalThreads;
        }
    }

    /**
     * 获取文件名。
     *
     * @param srcRealPath 文件路径
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
            String redirectedUrl = connection.getHeaderField("Location");
            connection.disconnect();
            if (redirectedUrl != null && !redirectedUrl.isEmpty()) {
                if (!redirectedUrl.startsWith("http://") && !redirectedUrl.startsWith("https://")) {
                    redirectedUrl = "http://" + redirectedUrl;
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
        Download.class.notifyAll();
    }
}