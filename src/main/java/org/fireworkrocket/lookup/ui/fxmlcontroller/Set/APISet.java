package org.fireworkrocket.lookup.ui.fxmlcontroller.Set;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyTableView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fireworkrocket.lookup.kernel.process.ApiParamHandler;
import org.fireworkrocket.lookup.ui.exception.ExceptionForwarder;
import org.fireworkrocket.lookup.kernel.config.DatabaseUtil;
import org.fireworkrocket.lookup.ui.JsonDataViewer;
import org.fireworkrocket.lookup.kernel.json_configuration.JSON_Data_Processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;

import static org.fireworkrocket.lookup.kernel.exception.ExceptionHandler.handleDebug;
import static org.fireworkrocket.lookup.kernel.process.PicProcessing.apiList;
import static org.fireworkrocket.lookup.kernel.process.net.util.URLUtil.*;

public class APISet {

    @FXML
    private MFXLegacyTableView<String> apiListView;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private MFXButton testApiButton;

    @FXML
    private MFXButton deleteApiButton;

    @FXML
    private MFXButton addApiParamButton;

    @FXML
    private MFXButton addApiButton;

    @FXML
    private TextField apiTextField;

    private ObservableList<String> apiObservableList;

    private final TableColumn<String, String> apiColumn = new TableColumn<>("API 列");

    private boolean isEditing = false;

    @FXML
    void initialize() {
        apiColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        apiListView.getColumns().add(apiColumn);

        new Thread(() -> {
            List<String> apiList = List.of(DatabaseUtil.getApiList());
            apiObservableList = FXCollections.observableArrayList(apiList);

            Platform.runLater(() -> {
                apiListView.setItems(apiObservableList);
            });
        }).start();

        apiListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleEnableApi();
            }
        });
    }
    @FXML
    private void handleAddApi() {
        Optional.ofNullable(apiTextField.getText())
                .filter(newApi -> !newApi.trim().isEmpty() && !apiObservableList.contains(newApi))
                .ifPresent(newApi -> {
                    apiObservableList.add(newApi);
                    DatabaseUtil.addItem(newApi);
                    updateApiList();
                    apiTextField.clear();
                });
    }

    @FXML
    private void handleDeleteApi() {
        Optional.ofNullable(apiListView.getSelectionModel().getSelectedItem())
                .ifPresent(selectedApi -> {
                    apiObservableList.remove(selectedApi);
                    apiListView.getItems().remove(selectedApi);
                    DatabaseUtil.deleteItem(selectedApi);
                });
    }

    private void handleEnableApi() {
        Optional.ofNullable(apiListView.getSelectionModel().getSelectedItem())
                .ifPresent(selectedApi -> {
                    if (selectedApi.endsWith("(已禁用)")) {
                        String enabledApi = selectedApi.substring(0, selectedApi.length() - 5);
                        apiListView.getItems().remove(selectedApi);
                        apiListView.getItems().add(enabledApi);
                        int index = apiObservableList.indexOf(selectedApi);
                        if (index != -1) {
                            apiObservableList.set(index, enabledApi);
                        }
                    }
                });
    }

    @FXML
    void handleAddApiParam(ActionEvent event) {
        toggleEditingMode();
        if (isEditing) {
            setupEditingMode();
        } else {
            resetEditingMode();
        }
    }

    private void toggleEditingMode() {
        isEditing = !isEditing;
        addApiButton.setText(isEditing ? "取消" : "添加");
        apiTextField.setEditable(!isEditing);
        deleteApiButton.setVisible(!isEditing);
    }

    private void setupEditingMode() {
        addApiParamButton.setText("应用");
        String selectedApi = apiListView.getSelectionModel().getSelectedItem();
        Map<String, String> params = parseURLParams(selectedApi);
        handleDebug("Params: " + params);
        if (selectedApi != null) {
            apiTextField.setText(selectedApi);
        }
        apiListView.getColumns().remove(apiColumn);
        setupParamTableColumns();
        populateParamTable(params);
    }

    private void resetEditingMode() {
        addApiParamButton.setText("添加参数");
        apiTextField.clear();
        apiListView.getColumns().clear();
        apiListView.getColumns().add(apiColumn);
        apiListView.getItems().clear();
        apiListView.getItems().addAll(apiObservableList);
    }

    private void setupParamTableColumns() {
        TableColumn<String, String> paramColumn = new TableColumn<>("参数名称（可能已在原始URL中定义）");
        TableColumn<String, String> valueColumn = new TableColumn<>("参数值");

        paramColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().split("=")[0]));
        valueColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().split("=")[1]));

        apiListView.getColumns().add(paramColumn);
        apiListView.getColumns().add(valueColumn);
        apiListView.getColumns().add(createEditColumn());
    }

    private void populateParamTable(Map<String, String> params) {
        ObservableList<String> paramList = FXCollections.observableArrayList();
        params.forEach((key, value) -> paramList.add(key + "=" + value));
        apiListView.setItems(paramList);
    }

    private TableColumn<String, Void> createEditColumn() {
        TableColumn<String, Void> editColumn = new TableColumn<>("操作");
        editColumn.setCellFactory(param -> new TableCell<>() {
            private final MFXButton editButton = new MFXButton("编辑");
            private final MFXButton deleteButton = new MFXButton("删除");
            private final MFXButton addButton = new MFXButton("新增");
            private final HBox hbox = new HBox(editButton, deleteButton, addButton); //将按钮放入HBox中
            private final MFXButton saveButton = new MFXButton("保存");
            private final TextField textField = new TextField();

            {
                editButton.setOnAction(event -> {
                    String selectedParam = getTableView().getItems().get(getIndex());
                    hbox.getChildren().clear();
                    textField.setPromptText("Example=xxx");
                    textField.setText(selectedParam);
                    textField.setPrefWidth(90);
                    hbox.getChildren().addAll(textField, saveButton);
                });

                saveButton.setOnAction(event -> {
                    try {
                        String selectedParam = getTableView().getItems().get(getIndex());
                        String newParam = textField.getText();
                        hbox.getChildren().clear();
                        getTableView().getItems().remove(selectedParam);
                        getTableView().getItems().add(newParam);
                        apiTextField.setText(ApiParamHandler.editParam(selectedParam, newParam, apiTextField.getText(), apiObservableList));
                        updateApiList();
                        getTableView().refresh();
                        hbox.getChildren().addAll(editButton, deleteButton, addButton);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                deleteButton.setOnAction(event -> {
                    String selectedParam = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(selectedParam);
                    apiTextField.setText(ApiParamHandler.deleteParam(selectedParam, apiTextField.getText(), apiObservableList));
                    updateApiList();
                    getTableView().refresh();
                });

                addButton.setOnAction(event -> {
                    hbox.getChildren().clear();
                    TextField keyField = new TextField();
                    keyField.setPromptText("Example=xxx");
                    keyField.setPrefWidth(90);
                    MFXButton saveButton = new MFXButton("保存");
                    saveButton.setOnAction(e -> {
                        try {
                            String newParam = keyField.getText().trim();
                            getTableView().getItems().add(newParam);
                            apiTextField.setText(ApiParamHandler.addParam(newParam, apiTextField.getText(), apiObservableList));
                            updateApiList();
                            hbox.getChildren().addAll(editButton, deleteButton, addButton);
                            getTableView().refresh();
                        } catch (Exception ex) {
                            ExceptionForwarder.handleException(ex);
                        }
                    });
                    hbox.getChildren().addAll(keyField, saveButton);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
        return editColumn;
    }

    private void updateApiList() {
        apiList = apiObservableList.toArray(new String[0]);
    }

    @FXML
    void createApiConfigFile() {
        new Thread(() -> {
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
                ExceptionForwarder.handleException(e);
            }
        }).start();
    }

    @FXML
    void testApi() {
        Platform.runLater(() -> testApiButton.setDisable(true));
        new Thread(() -> {
            int apiCount = 0;
            List<XYChart.Series<Number, Number>> timeSeriesList = new ArrayList<>();
            List<XYChart.Series<String, Number>> successRateSeriesList = new ArrayList<>();
            Map<String, List<String>> apiImageUrls = new HashMap<>();

            for (String apiUrl : apiList) {
                if (apiCount == 5) {
                    showStatisticsChart(timeSeriesList, successRateSeriesList, apiImageUrls);
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
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                HttpURLConnection imageConnection = JSON_Data_Processor.openConnection(imageUrl);
                                if (imageConnection.getResponseCode() == 200) {
                                    successCount++;
                                    imageUrls.add(imageUrl);
                                }
                            } else {
                                successCount++;
                            }
                        }
                    } catch (Exception e) {
                        ExceptionForwarder.handleException("API访问失败", e);
                    }
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    totalTime += duration;
                    timeSeries.getData().add(new XYChart.Data<>(i + 1, duration));
                }

                double successRate = (double) successCount / testCount * 100;
                long averageTime = totalTime / testCount;

                successRateSeries.getData().add(new XYChart.Data<>("成功率", successRate));

                timeSeriesList.add(timeSeries);
                successRateSeriesList.add(successRateSeries);
                apiImageUrls.put(apiUrl, imageUrls);
                apiCount++;
            }

            if (!timeSeriesList.isEmpty()) {
                showStatisticsChart(timeSeriesList, successRateSeriesList, apiImageUrls);
            }

            Platform.runLater(() -> testApiButton.setDisable(false));
        }).start();
    }

    private void showStatisticsChart(List<XYChart.Series<Number, Number>> timeSeriesList, List<XYChart.Series<String, Number>> successRateSeriesList, Map<String, List<String>> apiImageUrls) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("API访问统计");
            stage.setResizable(false);

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
    void testJson() {
        new Thread(JsonDataViewer::showJsonData).start();
    }
}