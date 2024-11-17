package org.fireworkrocket.lookup.FXMLController;

import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public interface UnDefinedControllerInterface {

    void setAnchorPane(AnchorPane anchorPane);

    AnchorPane getAnchorPane();

    void setLabelText(String text);

    String getLabelText();

}