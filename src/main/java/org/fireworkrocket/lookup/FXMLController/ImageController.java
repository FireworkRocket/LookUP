package org.fireworkrocket.lookup.FXMLController;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.TilePane;
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fireworkrocket.lookup.processor.DEFAULT_API_CONFIG.picNum;
import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.function.PicProcessing.getPic;

/**
 * ImageController 类，用于处理图片显示和交互。
 */
public class ImageController {

    @FXML
    private MFXButton reSetBgButton;

    @FXML
    private MFXButton refreshButton;

    @FXML
    private MFXScrollPane scrollPane;

    @FXML
    private TilePane showPicTilePane;

    private List<String> imageUrls; // 图片 URL 列表
    private int loadedImageCount = 0; // 已加载图片数量
    private static final int LOAD_BATCH_SIZE = 10; // 加载批次大小
    private final ExecutorService executorService = Executors.newFixedThreadPool(2); // 线程池
    private final WeakHashMap<String, ImageView> imageViewCache = new WeakHashMap<>(); // 缓存图片，避免重复加载
    private final Map<String, List<ImageView>> imageSizeMap = new HashMap<>(); // 按图片尺寸分类

    @FXML
    void initialize() {
        imageUrls = new ArrayList<>();
        executorService.submit(() -> refreshButtonAction(new ActionEvent())); // 刷新图片

        scrollPane.heightProperty().addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> showPicTilePane.setPrefSize(scrollPane.getWidth(), newValue.doubleValue())) // 设置 TilePane 大小
        );
    }

    @FXML
    void reSetBgAction(ActionEvent event) {
        executorService.submit(WallpaperChanger::getTodayWallpaper);
    }

    @FXML
    void refreshButtonAction(ActionEvent event) {
        executorService.submit(() -> {
            try {
                picNum = 70;
                imageUrls = getPic();
                Platform.runLater(() -> {
                    clearTilePane();
                    loadedImageCount = 0; // 重置已加载图片数量
                    loadMoreImages();
                    System.gc();
                });
            } catch (Exception e) {
                handleException(e);
            }
        });
    }

    @FXML
    void showPicTilePaneAction(ScrollEvent event) {
        if (event.getDeltaY() > 0) {
            return; // 向上滚动
        }
        if (!showPicTilePane.getChildren().isEmpty() &&
                showPicTilePane.getChildren().size() - showPicTilePane.getChildren().indexOf(showPicTilePane.getChildren().get(showPicTilePane.getChildren().size() - 1))
                        < LOAD_BATCH_SIZE) { // 用总大小减去最后一个元素的索引，如果小于加载批次大小，则加载更多图片
            loadMoreImages();
        }
    }

    private void clearTilePane() {
        showPicTilePane.getChildren().forEach(node -> { // 清空图片
            if (node instanceof ImageView) {
                ((ImageView) node).setImage(null);
            }
        });
        showPicTilePane.getChildren().clear();
        System.gc(); // 清理 JVM 垃圾
    }

    private void loadMoreImages() {
        int end = Math.min(loadedImageCount + LOAD_BATCH_SIZE, imageUrls.size()); // 计算加载结束位置
        for (int i = loadedImageCount; i < end; i++) {
            String url = imageUrls.get(i); // 获取图片URL
            executorService.submit(() -> {
                ImageView imageView = imageViewCache.get(url); // 从缓存中获取图片
                if (imageView == null) {
                    imageView = new ImageView();
                    loadImageIntoImageView(url, imageView); // 加载图片
                    imageViewCache.put(url, imageView); // 缓存图片
                }
                ImageView finalImageView = imageView;
                Platform.runLater(() -> {
                    if (!showPicTilePane.getChildren().contains(finalImageView)) {
                        showPicTilePane.getChildren().add(finalImageView); // 添加图片
                    }
                });
            });
        }
        loadedImageCount = end; // 更新已加载图片数量
    }

    private void loadImageIntoImageView(String url, ImageView imageView) {
        try {
            URL imageUrl = new URI(url).toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) imageUrl.openConnection(); // 将 URL 转换为 HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
            System.out.println(
                    "URL:" + connection.getURL() + " " +
                            connection.getResponseCode() + "\n" +
                            "MSG:" + connection.getResponseMessage() + "\n" +
                            "TYPE:" + connection.getContentType() + "\n" +
                            "Length:" + connection.getContentLength() + "\n" +
                            connection.getRequestMethod() + "\n"
            );
            try (InputStream inputStream = connection.getInputStream()) {
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream); // 创建图片输入流
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream); // 获取图片读取器
                if (!readers.hasNext()) {
                    throw new IOException("No ImageReader found for URL: " + url); // 未找到图片读取器
                }
                ImageReader reader = readers.next(); // 获取第一个图片读取器
                reader.setInput(imageInputStream, true, true); // 设置输入流
                BufferedImage originalImage = reader.read(0); // 读取图片
                if (originalImage == null) {
                    throw new IOException("ImageIO read returned null for URL: " + url);
                }
                javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(originalImage, null);
                Platform.runLater(() -> {
                    imageView.setImage(fxImage);
                    imageView.setFitHeight(150); // 设置固定高度
                    imageView.setFitWidth(150);  // 设置固定宽度
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setCache(true);

                    String sizeKey = fxImage.getWidth() + "x" + fxImage.getHeight(); // 按尺寸分类
                    imageSizeMap.computeIfAbsent(sizeKey, k -> new ArrayList<>()).add(imageView); // 添加到分类
                    updateTilePane(); // 更新 TilePane
                });
            }
        } catch (Exception e) {
            System.err.println("IOException while loading image from URL: " + url);
            handleException(e);
        }
    }

    private void updateTilePane() {
        showPicTilePane.getChildren().clear();
        showPicTilePane.setHgap(10); // 设置水平间距
        showPicTilePane.setVgap(10); // 设置垂直间距
        showPicTilePane.setPadding(new Insets(10)); // 设置填充
        imageSizeMap.values().forEach(showPicTilePane.getChildren()::addAll); // 添加所有图片
    }
}