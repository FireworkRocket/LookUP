package org.fireworkrocket.lookup.FXMLController;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class SettingController {

    @FXML
    private MFXButton API_Enabled_List;

    @FXML
    private MFXToggleButton Disabled_REF_Time;

    @FXML
    private MFXFilterComboBox<?> REF_Time;

    @FXML
    private MFXButton Set_API_Config;

    @FXML
    private MFXButton TestAPI;

    @FXML
    private Text Vertext;

    @FXML
    private Accordion accordion;

    @FXML
    private MFXButton re_SetAPI_R_Text;

    @FXML
    void EGG(MouseEvent event) {

    }

    @FXML
    void REF_Time(ActionEvent event) {

    }

    @FXML
    void TestAPI(ActionEvent event) {

    }

    @FXML
    void re_SetAPI_R_Text(ActionEvent event) {

    }

}
