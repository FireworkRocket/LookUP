package org.fireworkrocket.lookup.FXMLController;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

/**
 * HomeController 类，用于处理主页的交互。
 */
public class HomeController {

    @FXML
    private AnchorPane homeAnchorPane;

    @FXML
    private MFXButton picButton;

    @FXML
    private MFXButton vcrButton;

    @FXML
    private MFXButton setButton;

    /**
     * 处理图片按钮的点击事件。
     *
     * @param event 事件对象
     */
    @FXML
    void handlePicButtonAction(ActionEvent event) {
        FXMLLoaderUtil.loadFXML("Image.fxml", homeAnchorPane);
    }

    /**
     * 处理VCR按钮的点击事件。
     *
     * @param event 事件对象
     */
    @FXML
    void handleVcrButtonAction(ActionEvent event) {
        // 根据需要实现
    }

    /**
     * 处理设置按钮的点击事件。
     *
     * @param event 事件对象
     */
    @FXML
    void handleSetButtonAction(ActionEvent event) {
        FXMLLoaderUtil.loadFXML("Setting.fxml", homeAnchorPane);
    }
}