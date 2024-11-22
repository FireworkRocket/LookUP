package org.fireworkrocket.lookup.FXMLController;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXListView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingConteoller {


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
    void APISettings(ActionEvent event) {
        FXMLLoaderUtil.loadFXML("Set/APISet.fxml", homeAnchorPane);
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
    void refreshInterval(ActionEvent event) {

    }

    @FXML
    void initialize() {

    }

}