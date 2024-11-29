package org.fireworkrocket.lookup.fxmlcontroller;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.ui.wallpaper.WallpaperChanger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.fireworkrocket.lookup.kernel.config.DefaultConfig.getPicNum;
import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.kernel.process.PicProcessing.getPicAtNow;
import static org.fireworkrocket.lookup.kernel.process.PicProcessing.picProcessingShutdown;

public class ImageController {

    @FXML
    private MFXButton reSetBgButton;

    @FXML
    private MFXButton refreshButton;

    @FXML
    private MFXScrollPane scrollPane;

    @FXML
    private TilePane showPicTilePane;

    private List<String> imageUrls;

    private int loadedImageCount = 0;
    private static final int LOAD_BATCH_SIZE = DefaultConfig.loadBatchSize;
    private final ExecutorService executorService = Executors.newFixedThreadPool(DefaultConfig.threadPoolSize);
    private final Map<String, SoftReference<ImageView>> imageViewCache = Collections.synchronizedMap(new HashMap<>());
    private final PauseTransition pauseTransition = new PauseTransition(Duration.millis(DefaultConfig.pauseTransitionMillis));
    private final PauseTransition debounceTransition = new PauseTransition(Duration.millis(DefaultConfig.debounceTransitionMillis));
    HomeController homeController = HomeController.getInstance();

    @FXML
    void initialize() {
        if (homeController != null) {
            homeController.setGoldProgress(-1);
        }

        imageUrls = Collections.synchronizedList(new ArrayList<>());

        scrollPane.heightProperty().addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> showPicTilePane.setPrefSize(scrollPane.getWidth()-20, newValue.doubleValue()))
        );

        pauseTransition.setOnFinished(event -> loadMoreImages());

        // 在初始化时自动加载图片
        refreshButtonAction(new ActionEvent());
    }

    @FXML
    void reSetBgAction(ActionEvent event) {
        reSetBgButton.setDisable(true);
        executorService.submit(() -> {
            try {
                WallpaperChanger.getTodayWallpaper();
            } finally {
                Platform.runLater(() -> reSetBgButton.setDisable(false));
            }
        });
    }

    @FXML
    void refreshButtonAction(ActionEvent event) {
        homeController.setGoldProgress(-1);
        clearTilePane(); // 清空旧图片
        getImageList();
    }

    @FXML
    void showPicTilePaneAction(ScrollEvent event) {
        if (event.getDeltaY() > 0) {
            return;
        }
        if (!showPicTilePane.getChildren().isEmpty() &&
                showPicTilePane.getChildren().size() - showPicTilePane.getChildren().indexOf(showPicTilePane.getChildren().getLast())
                        < LOAD_BATCH_SIZE) {
            if (!pauseTransition.getStatus().equals(PauseTransition.Status.RUNNING)) {
                pauseTransition.playFromStart();
            }
        }
    }

    private void clearTilePane() {
        showPicTilePane.getChildren().forEach(node -> {
            if (node instanceof ImageView) {
                ((ImageView) node).setImage(null);
            }
        });
        showPicTilePane.getChildren().clear();
        imageViewCache.clear();
        System.gc();
    }

    private void loadMoreImages() {
        if (!DefaultConfig.auto_Load_Image){
            int end = Math.min(loadedImageCount + LOAD_BATCH_SIZE, imageUrls.size());
            for (int i = loadedImageCount; i < end; i++) {
                String url = imageUrls.get(i);
                if (!imageViewCache.containsKey(url) || imageViewCache.get(url).get() == null) {
                    loadImage(url);
                }
            }
            loadedImageCount = end;
        } else {
            int visibleRows = (int) Math.ceil(scrollPane.getHeight() / getLastImageViewHeight()); // 可见行数
            int additionalRows = DefaultConfig.imageadditionalRows;
            int imagesPerRow = (int) Math.ceil(scrollPane.getWidth() / getLastImageViewWidth()); // 每行图片数
            int totalImagesToLoad = (visibleRows + additionalRows) * imagesPerRow; // 预加载图片总数

            loadedImageCount = 0; // 重置已加载图片计数
            imageUrls.clear(); // 清空旧的图片 URL 列表
            // 重新加载新的图片
            getImageList();
            loadedImageCount = totalImagesToLoad;
        }
    }

    private void getImageList() {
        debounceTransition.setOnFinished(_ -> executorService.submit(() -> {
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < getPicNum; i++) {
                    futures.add(getPicAtNow().thenAccept(url -> {
                        if (url != null) {
                            synchronized (imageUrls) {
                                if (!imageUrls.contains(url)) {
                                    imageUrls.add(url);
                                    Platform.runLater(() -> {
                                        loadImage(url);
                                        loadedImageCount++;
                                    });
                                }
                            }
                        }
                    }));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                Platform.runLater(() -> homeController.setGoldProgress(0)); // 加载完成后设置进度
            } catch (Exception e) {
                handleException(e);
            }
        }));
        debounceTransition.playFromStart();
    }

    private double getLastImageViewWidth() {
        if (!showPicTilePane.getChildren().isEmpty()) {
            ImageView lastImageView = (ImageView) showPicTilePane.getChildren().getLast();
            return lastImageView.getFitWidth();
        }
        return 150; // 默认宽度
    }

    private double getLastImageViewHeight() {
        if (!showPicTilePane.getChildren().isEmpty()) {
            ImageView lastImageView = (ImageView) showPicTilePane.getChildren().getLast();
            return lastImageView.getFitHeight();
        }
        return 150; // 默认高度
    }

    private void loadImage(String url) {
        SoftReference<ImageView> imageViewRef = imageViewCache.get(url);
        ImageView cachedImageView = (imageViewRef != null) ? imageViewRef.get() : null;

        if (cachedImageView != null) {
            if (!showPicTilePane.getChildren().contains(cachedImageView)) {
                showPicTilePane.getChildren().add(cachedImageView);
            }
        } else {
            imageViewCache.putIfAbsent(url, new SoftReference<>(null));
            submitLoadImageTask(url);
        }
        cleanUpCache();
    }

    private void submitLoadImageTask(String url) {
        Task<ImageView> loadImageTask = new Task<>() {
            @Override
            protected ImageView call() throws Exception {
                return loadThumbnail(url);
            }

            @Override
            protected void succeeded() {
                ImageView imageView = getValue();
                if (imageView != null) {
                    Platform.runLater(() -> {
                        if (!showPicTilePane.getChildren().contains(imageView)) {
                            showPicTilePane.getChildren().add(imageView);
                            imageViewCache.put(url, new SoftReference<>(imageView));
                        }
                    });
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                Platform.runLater(() -> handleException(exception));
                imageUrls.remove(url); // 移除加载失败的图片 URL
            }
        };
        executorService.submit(loadImageTask);
    }

    private void cleanUpCache() {
        imageViewCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }

    private ImageView loadThumbnail(String url) throws Exception {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        SoftReference<ImageView> imageViewRef = imageViewCache.get(url);
        ImageView imageView = (imageViewRef != null) ? imageViewRef.get() : null;
        if (imageView == null) {
            imageView = new ImageView();
            URL imageUrl = new URI(url).toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) imageUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.58 Safari/537.36");
            try (InputStream inputStream = connection.getInputStream()) {
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
                if (!readers.hasNext()) {
                    throw new IOException("No ImageReader found for URL: " + url);
                }
                ImageReader reader = readers.next();
                reader.setInput(imageInputStream, true, true);
                BufferedImage originalImage = reader.read(0);
                if (originalImage == null) {
                    throw new IOException("ImageIO read returned null for URL: " + url);
                }
                BufferedImage thumbnail = createThumbnail(originalImage);
                javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(thumbnail, null);
                imageView.setImage(fxImage);
                imageView.setFitHeight(150);
                imageView.setFitWidth(150);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setCache(true);
                imageViewCache.put(url, new SoftReference<>(imageView));
            }
        }
        return imageView;
    }

    private BufferedImage createThumbnail(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int newWidth = 150;
        int newHeight = (newWidth * height) / width;
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        return thumbnail;
    }

    @FXML
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        picProcessingShutdown();
    }
}