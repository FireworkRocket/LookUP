package org.fireworkrocket.lookup.FXMLController;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.fireworkrocket.lookup.exception.ExceptionHandler;

import java.io.IOException;

public class HomeController {

    @FXML
    public AnchorPane homeAnchorPane;

    @FXML
    private HBox HomeHBox;

    @FXML
    private MFXButton picButton;

    @FXML
    private MFXButton vcrButton;

    @FXML
    private MFXButton setButton;

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
            ExceptionHandler.handleException(e);
        }
    }

}