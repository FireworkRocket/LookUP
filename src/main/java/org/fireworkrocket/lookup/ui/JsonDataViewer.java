package org.fireworkrocket.lookup.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import org.fireworkrocket.lookup.kernel.json_configuration.JSON_Data_Processor;

import java.util.Map;
import java.util.Optional;

public class JsonDataViewer {

    public static void showJsonData() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("输入URL");
            dialog.setHeaderText("请输入要获取JSON数据的URL");
            dialog.setContentText("URL:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(url -> {
                try {
                    Map<String, Object> jsonData = JSON_Data_Processor.getUrl(url);
                    StringBuilder dataString = new StringBuilder();
                    jsonData.forEach((key, value) -> dataString.append(key).append(": ").append(value).append("\n"));

                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("JSON Data Viewer");
                    alert.setHeaderText("JSON Data from URL");
                    alert.setContentText(dataString.toString());
                    alert.showAndWait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}