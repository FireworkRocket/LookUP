package org.fireworkrocket.lookup.FXMLController;

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
import org.fireworkrocket.lookup.function.wallpaperchanger.WallpaperChanger;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;
import static org.fireworkrocket.lookup.function.PicProcessing.*;
import static org.fireworkrocket.lookup.processor.DEFAULT_API_CONFIG.picNum;

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
    private static final int LOAD_BATCH_SIZE = 10;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Map<String, SoftReference<ImageView>> imageViewCache = Collections.synchronizedMap(new HashMap<>());
    private final PauseTransition pauseTransition = new PauseTransition(Duration.millis(100));

    @FXML
    void initialize() {
        imageUrls = Collections.synchronizedList(new ArrayList<>());

        scrollPane.heightProperty().addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> showPicTilePane.setPrefSize(scrollPane.getWidth(), newValue.doubleValue()))
        );

        pauseTransition.setOnFinished(event -> loadMoreImages());

        // 在初始化时自动加载图片
        refreshButtonAction(new ActionEvent());
    }

    @FXML
    void reSetBgAction(ActionEvent event) {
        executorService.submit(WallpaperChanger::getTodayWallpaper);
    }

    @FXML
    void refreshButtonAction(ActionEvent event) {
        executorService.submit(() -> {
            try {
                picNum = 10;
                imageUrls.clear();
                for (int i = 0; i < picNum; i++) {
                    String url = getPicAtNow();
                    imageUrls.add(url);
                    Platform.runLater(() -> loadImage(url));
                }
                Platform.runLater(() -> {
                    clearTilePane();
                    loadedImageCount = 0;
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
        int end = Math.min(loadedImageCount + LOAD_BATCH_SIZE, imageUrls.size());
        for (int i = loadedImageCount; i < end; i++) {
            String url = imageUrls.get(i);
            loadImage(url);
        }
        loadedImageCount = end;
    }

    private void loadImage(String url) {
        Platform.runLater(() -> {
            if (imageViewCache.containsKey(url) && imageViewCache.get(url).get() != null) {
                ImageView cachedImageView = imageViewCache.get(url).get();
                if (!showPicTilePane.getChildren().contains(cachedImageView)) {
                    showPicTilePane.getChildren().add(cachedImageView);
                }
            } else {
                if (!imageViewCache.containsKey(url)) {
                    imageViewCache.put(url, new SoftReference<>(null));
                }
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
                        if (exception instanceof Exception) {
                            Platform.runLater(() -> handleException(exception));
                        } else if (exception instanceof Error) {
                            Platform.runLater(() -> handleException(new Exception("An error occurred: " + exception.getMessage(), exception)));
                        }
                    }
                };
                executorService.submit(loadImageTask);
            }
        });
    }

    private ImageView loadThumbnail(String url) throws Exception {
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