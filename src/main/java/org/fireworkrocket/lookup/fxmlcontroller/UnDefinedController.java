package org.fireworkrocket.lookup.fxmlcontroller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class UnDefinedController implements UnDefinedControllerInterface {

    @FXML
    private AnchorPane DefinedAnchorPane;

    @FXML
    private Label LittleText;

    @Override
    public void setAnchorPane(AnchorPane anchorPane) {
        this.DefinedAnchorPane = anchorPane;
    }

    @Override
    public AnchorPane getAnchorPane() {
        return DefinedAnchorPane;
    }

    @Override
    public void setLabelText(String text) {
        this.LittleText.setText(text);
    }

    @Override
    public String getLabelText() {
        return LittleText.getText();
    }
}