package org.fireworkrocket.lookup.ui.fxmlcontroller.Set;

import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;

public class Refresh {

    @FXML
    private MFXCheckbox autoRefresh;

    @FXML
    private MFXToggleButton enableSettingWallpaper;

    @FXML
    private MFXDatePicker refreshDate;

    @FXML
    private MFXComboBox<?> refreshInterval;

    @FXML
    void autoRefresh(ActionEvent event) {
        ExceptionHandler.handleInfo("下次一定(oڡo )");
    }

    @FXML
    void refreshDate(ActionEvent event) {

    }

    @FXML
    void refreshInterval(ActionEvent event) {
        ExceptionHandler.handleInfo("下次一定(oڡo )");
    }

    void initialize() {

    }

}
