package org.fireworkrocket.lookup.FXMLController.Set;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fireworkrocket.lookup.exception.ExceptionHandler;
import org.fireworkrocket.lookup.processor.JSON_Read_Configuration.JsonDataViewer;
import org.fireworkrocket.lookup.processor.JSON_Read_Configuration.JSON_Data_Processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;

import static org.fireworkrocket.lookup.function.PicProcessing.apiList;
import static org.fireworkrocket.lookup.function.PicProcessing.getDisabledApis;

public class APISet {

    @FXML
    private MFXListView<String> APIListView;

    @FXML
    private MFXButton TestAPIButton;

    @FXML
    private TextField apiTextField;

    private ObservableList<String> apiObservableList;

    @FXML
    void initialize() {
        List<String> DisabledApis = getDisabledApis();
        apiObservableList = FXCollections.observableArrayList(apiList);
        if (DisabledApis.isEmpty()){
            APIListView.setItems(apiObservableList);
        } else {
            for (String Now : apiObservableList) {
                if (DisabledApis.contains(Now)){
                    APIListView.getItems().add(Now+"(已禁用)");
                } else {
                    APIListView.getItems().add(Now);
                }
            }
        }

        APIListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleEnableAPI();
            }
        });
    }


    @FXML
    private void handleAddAPI() {
        Optional.ofNullable(apiTextField.getText())
                .filter(newApi -> !newApi.trim().isEmpty() && !apiObservableList.contains(newApi))
                .ifPresent(newApi -> {
                    apiObservableList.add(newApi);
                    apiTextField.clear();
                    updateApiList();
                });
    }

    @FXML
    private void handleDeleteAPI() {
        Optional.ofNullable(APIListView.getSelectionModel().getSelectedValues().getFirst())
                .ifPresent(selectedApi -> {
                    apiObservableList.remove(selectedApi);
                    updateApiList();
                });
    }

    private void handleEnableAPI() {
        Optional.ofNullable(APIListView.getSelectionModel().getSelectedValues().getFirst())
                .ifPresent(selectedApi -> {
                    if (selectedApi.endsWith("(已禁用)")) {
                        String enabledApi = selectedApi.substring(0, selectedApi.length() - 5);
                        APIListView.getItems().remove(selectedApi);
                        APIListView.getItems().add(enabledApi);
                        int index = apiObservableList.indexOf(selectedApi);
                        if (index != -1) {
                            apiObservableList.set(index, enabledApi);
                        }
                    }
                });
    }

    private void updateApiList() {
        apiList = apiObservableList.toArray(new String[0]);
    }

    @FXML
    void CreateAPiConfigFile() {
        try {
            File configDir = new File("Config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File configFile = new File(configDir, "json_formats.json");
            if (!configFile.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
                    writer.write("{\n" +
                            "  \"formats\": [\n" +
                            "    {\n" +
                            "      \"status\": \"status\",\n" +
                            "      \"data\": \"data\",\n" +
                            "      \"url\": \"url\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"status\": \"code\",\n" +
                            "      \"data\": \"data\",\n" +
                            "      \"url\": \"imageUrl\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
                }
            }

            Runtime.getRuntime().exec("notepad " + configFile.getAbsolutePath());
        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        }
    }

    @FXML
    void TestAPI() {
        Platform.runLater(() -> TestAPIButton.setDisable(true));
        new Thread(() -> {
            int apiCount = 0;
            List<XYChart.Series<Number, Number>> timeSeriesList = new ArrayList<>();
            List<XYChart.Series<String, Number>> successRateSeriesList = new ArrayList<>();
            Map<String, List<String>> apiImageUrls = new HashMap<>();

            for (String apiUrl : apiList) {
                if (apiCount == 5) {
                    List<XYChart.Series<Number, Number>> finalTimeSeriesList = new ArrayList<>(timeSeriesList);
                    List<XYChart.Series<String, Number>> finalSuccessRateSeriesList = new ArrayList<>(successRateSeriesList);
                    Map<String, List<String>> finalApiImageUrls = new HashMap<>(apiImageUrls);
                    Platform.runLater(() -> showStatisticsChart(finalTimeSeriesList, finalSuccessRateSeriesList, finalApiImageUrls));
                    timeSeriesList.clear();
                    successRateSeriesList.clear();
                    apiImageUrls.clear();
                    apiCount = 0;
                }

                XYChart.Series<Number, Number> timeSeries = new XYChart.Series<>();
                timeSeries.setName(apiUrl);

                XYChart.Series<String, Number> successRateSeries = new XYChart.Series<>();
                successRateSeries.setName(apiUrl);

                int successCount = 0;
                int failureCount = 0;
                long totalTime = 0;
                int testCount = 10;

                List<String> imageUrls = new ArrayList<>();

                for (int i = 0; i < testCount; i++) {
                    long startTime = System.currentTimeMillis();
                    try {
                        HttpURLConnection connection = JSON_Data_Processor.openConnection(apiUrl);
                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            String imageUrl = JSON_Data_Processor.getUrl(String.valueOf(connection.getURL())).get("URL").toString();
                            System.out.println("Image URL: " + imageUrl);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                HttpURLConnection imageConnection = JSON_Data_Processor.openConnection(imageUrl);
                                if (imageConnection.getResponseCode() == 200) {
                                    successCount++;
                                    imageUrls.add(imageUrl);
                                } else {
                                    failureCount++;
                                }
                            } else {
                                successCount++;
                            }
                        } else {
                            failureCount++;
                        }
                    } catch (Exception e) {
                        failureCount++;
                        ExceptionHandler.handleException("API访问失败", e);
                    }
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    totalTime += duration;
                    timeSeries.getData().add(new XYChart.Data<>(i + 1, duration));
                }

                double successRate = (double) successCount / testCount * 100;
                long averageTime = totalTime / testCount;

                successRateSeries.getData().add(new XYChart.Data<>("成功率", successRate));

                System.out.println("API URL: " + apiUrl);
                System.out.println("成功率: " + successRate + "%");
                System.out.println("平均访问时间: " + averageTime + " ms");

                timeSeriesList.add(timeSeries);
                successRateSeriesList.add(successRateSeries);
                apiImageUrls.put(apiUrl, imageUrls);
                apiCount++;
            }

            if (!timeSeriesList.isEmpty()) {
                List<XYChart.Series<Number, Number>> finalTimeSeriesList = new ArrayList<>(timeSeriesList);
                List<XYChart.Series<String, Number>> finalSuccessRateSeriesList = new ArrayList<>(successRateSeriesList);
                Map<String, List<String>> finalApiImageUrls = new HashMap<>(apiImageUrls);
                Platform.runLater(() -> showStatisticsChart(finalTimeSeriesList, finalSuccessRateSeriesList, finalApiImageUrls));
            }

            Platform.runLater(() -> TestAPIButton.setDisable(false));
        }).start();
    }

    private void showStatisticsChart(List<XYChart.Series<Number, Number>> timeSeriesList, List<XYChart.Series<String, Number>> successRateSeriesList, Map<String, List<String>> apiImageUrls) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("API访问统计");
            stage.setResizable(false); // 禁用最大化

            final NumberAxis xAxisTime = new NumberAxis(0, 10, 1);
            final NumberAxis yAxisTime = new NumberAxis();
            xAxisTime.setLabel("测试次数");
            yAxisTime.setLabel("访问时间 (ms)");

            final LineChart<Number, Number> lineChart = new LineChart<>(xAxisTime, yAxisTime);
            lineChart.setTitle("API访问时间统计");

            for (XYChart.Series<Number, Number> series : timeSeriesList) {
                lineChart.getData().add(series);
            }

            final CategoryAxis xAxisSuccess = new CategoryAxis();
            final NumberAxis yAxisSuccess = new NumberAxis(0, 100, 10);
            xAxisSuccess.setLabel("API");
            yAxisSuccess.setLabel("成功率 (%)");

            final BarChart<String, Number> barChart = new BarChart<>(xAxisSuccess, yAxisSuccess);
            barChart.setTitle("图片下载成功率");

            for (XYChart.Series<String, Number> series : successRateSeriesList) {
                barChart.getData().add(series);
            }

            TreeView<String> imageUrlTreeView = new TreeView<>();
            TreeItem<String> rootItem = new TreeItem<>("API Image URLs");
            rootItem.setExpanded(true);

            for (Map.Entry<String, List<String>> entry : apiImageUrls.entrySet()) {
                String apiUrl = entry.getKey();
                TreeItem<String> apiItem = new TreeItem<>(apiUrl);
                for (String imageUrl : entry.getValue()) {
                    TreeItem<String> imageUrlItem = new TreeItem<>(imageUrl);
                    apiItem.getChildren().add(imageUrlItem);
                }
                rootItem.getChildren().add(apiItem);
            }

            imageUrlTreeView.setRoot(rootItem);

            VBox vbox = new VBox(lineChart, barChart, imageUrlTreeView);
            Scene scene = new Scene(vbox, 800, 800);
            stage.setScene(scene);

            // 添加关闭事件处理器
            stage.setOnCloseRequest(event -> {
                lineChart.getData().clear();
                barChart.getData().clear();
                imageUrlTreeView.setRoot(null);
                System.gc();
            });

            stage.show();
        });
    }
    @FXML
    void TestJSON() {
        new Thread(JsonDataViewer::showJsonData).start();
    }
}