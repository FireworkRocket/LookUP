package org.fireworkrocket.lookup.ui.fxmlcontroller;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

public class SettingConteoller {



    @FXML
    private AnchorPane SetPane;

    @FXML
    private AnchorPane SetAnchorPane;

    @FXML
    private MFXButton APISettings;

    @FXML
    private MFXButton generalSettings;

    @FXML
    private MFXButton refreshInterval;

    @FXML
    private MFXButton checkForUpdates;

    @FXML
    private MFXButton about;

    private AnchorPane homeAnchorPane;

    public void setHomeAnchorPane(AnchorPane homeAnchorPane) {
        this.homeAnchorPane = homeAnchorPane;
    }

    @FXML
    void APISettings(ActionEvent event) {FXMLLoaderUtil.loadFXML("Set/APISet.fxml", SetPane);}

    @FXML
    void refreshInterval(ActionEvent event) {
        FXMLLoaderUtil.loadFXML("Set/Refresh.fxml", SetPane);
    }

    @FXML
    void generalSettings(ActionEvent event) {

    }

    @FXML
    void checkForUpdates(ActionEvent event) {

    }

    @FXML
    void about(ActionEvent event) {

    }

    @FXML
    void initialize() {

    }

}