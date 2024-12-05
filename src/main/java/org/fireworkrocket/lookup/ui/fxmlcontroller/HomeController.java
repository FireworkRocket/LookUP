package org.fireworkrocket.lookup.ui.fxmlcontroller;

import com.luciad.imageio.webp.WebPImageReaderSpi;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.process.PicProcessing;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleWarning;
import static org.fireworkrocket.lookup.ui.exception.ExceptionForwarder.handleException;
import static org.fireworkrocket.lookup.ui.fxmlcontroller.Set.Today.today;

public class HomeController {

    @FXML
    private MFXProgressBar GoldProgress;

    @FXML
    private HBox HomeHbox;

    @FXML
    public AnchorPane homeAnchorPane;

    @FXML
    private ImageView Background;

    @FXML
    private AnchorPane Scene;

    @FXML
    private MFXButton picButton;

    @FXML
    private MFXButton vcrButton;

    @FXML
    private MFXButton setButton;

    private static HomeController instance;

    @FXML
    void handlePicButtonAction(ActionEvent event) {
        FXMLLoaderUtil.loadFXML("Image.fxml", homeAnchorPane);
    }

    @FXML
    void handleVcrButtonAction(ActionEvent event) {
        //仍在测试中
    }

    @FXML
    void handleSetButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Set.fxml"));
            AnchorPane setPane = loader.load();
            SettingConteoller settingController = loader.getController();
            settingController.setHomeAnchorPane(homeAnchorPane);
            homeAnchorPane.getChildren().clear(); // 清理旧内容
            homeAnchorPane.getChildren().setAll(setPane);
            AnchorPane.setTopAnchor(setPane, 0.0);
            AnchorPane.setBottomAnchor(setPane, 0.0);
            AnchorPane.setLeftAnchor(setPane, 0.0);
            AnchorPane.setRightAnchor(setPane, 0.0);
        } catch (IOException e) {
            handleException(e);
        }
    }

    @FXML
    void handleHomeButtonAction(ActionEvent actionEvent) {

    }

    @FXML
    void initialize() {
        instance = this;
        GoldProgress.setProgress(0);

        AtomicReference<Image> image = new AtomicReference<>(new Image(DefaultConfig.backGroundfile.toURI().toString()));

        if (image.get().isError()) {
            Thread imageLoaderThread = new Thread(() -> {
                // 注册WebP ImageIO插件
                IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
                ImageIO.scanForPlugins();
                ImageIO.setUseCache(false);
                try {
                    PicProcessing.picNum = 1;
                    image.set(new Image(PicProcessing.getPic().getFirst()));

                    if (image.get().isError()) {
                        handleWarning("壁纸可能加载失败 " + image.get().getException().getMessage());
                    }

                    Platform.runLater(() -> {
                        Background.setImage(image.get());
                        image.set(null);
                    });

                } catch (Exception e) {
                    handleException(e);
                }
            });
            imageLoaderThread.setDaemon(true); // 设置为守护线程，确保应用程序退出时线程自动终止
            imageLoaderThread.start();
        } else {
            Background.setImage(image.get());
        }

        Platform.runLater(() -> {
            Scene.widthProperty().addListener((_, _, newVal) -> Background.setFitWidth(newVal.doubleValue()));
            Scene.heightProperty().addListener((_, _, newVal) -> Background.setFitHeight(newVal.doubleValue()));
            Background.setFitWidth(Scene.getWidth());
            Background.setFitHeight(Scene.getHeight());
            Background.setSmooth(true);
            Background.setPreserveRatio(false);
        });

        FXMLLoaderUtil.loadFXML("Today.fxml", homeAnchorPane);

        if (false) {
            // 获取背景图像的平均颜色
            if (!image.get().isError()){
                Color averageColor = getAverageColor(image.get());
                // 计算反色
                String invertedColor = invertColor(averageColor);
                today.WelconeLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: " + invertedColor + ";");
            }
        }

        image.set(null);
    }

    private Color getAverageColor(Image image) {
        PixelReader pixelReader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        long r = 0, g = 0, b = 0;
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                r += (long) (color.getRed() * 255);
                g += (long) (color.getGreen() * 255);
                b += (long) (color.getBlue() * 255);
                count++;
            }
        }

        return Color.rgb((int) (r / count), (int) (g / count), (int) (b / count));
    }

    private String invertColor(Color color) {
        int r = (int) (255 - color.getRed() * 255);
        int g = (int) (255 - color.getGreen() * 255);
        int b = (int) (255 - color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static HomeController getInstance() {
        return instance;
    }

    public void setGoldProgress(double progress) {
        if (GoldProgress != null) {
            Platform.runLater(() -> {
                if (progress == -1) {
                    GoldProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                } else {
                    GoldProgress.setProgress(progress);
                }
            });
        }
    }
}