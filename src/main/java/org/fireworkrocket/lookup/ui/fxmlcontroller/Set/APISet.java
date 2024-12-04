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
    private MFXLegacyTableView<String> APIListView;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private MFXButton TestAPIButton;

    @FXML
    private MFXButton deleteAPIButton;

    @FXML
    private MFXButton handleAddAPIParam;

    @FXML
    private MFXButton handleAddAPI;

    @FXML
    private TextField apiTextField;

    private ObservableList<String> apiObservableList; // API列表

    // 创建列
    TableColumn<String, String> apiColumn = new TableColumn<>("API 列");

    @FXML
    void initialize() {
        apiColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));

        // 将列添加到表中
        APIListView.getColumns().add(apiColumn);

        // 设置表数据
        List<String> apiList = List.of(DatabaseUtil.getApiList());
        apiObservableList = FXCollections.observableArrayList(apiList);
        APIListView.setItems(apiObservableList);

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
                    DatabaseUtil.addItem(newApi);
                    updateApiList(); // 更新API列表
                    apiTextField.clear();
                });
    }

    @FXML
    private void handleDeleteAPI() {
        Optional.ofNullable(APIListView.getSelectionModel().getSelectedItem())
                .ifPresent(selectedApi -> {
                    apiObservableList.remove(selectedApi);
                    APIListView.getItems().remove(selectedApi);
                    DatabaseUtil.deleteItem(selectedApi);
                });
    }

    private void handleEnableAPI() {
        Optional.ofNullable(APIListView.getSelectionModel().getSelectedItem())
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

    boolean isEditing = false; // 是否正在编辑API参数
    @FXML
    void handleAddAPIParam(ActionEvent event) {
        handleAddAPI.setText("取消");
        if (isEditing) {
            isEditing = false;
            apiTextField.setEditable(true);
            apiTextField.clear();
            handleAddAPIParam.setText("添加参数");
            handleAddAPI.setText("添加");
            APIListView.getColumns().clear();
            APIListView.getItems().clear();
            APIListView.getColumns().add(apiColumn);
            APIListView.getItems().addAll(apiObservableList);
            return;
        }
        isEditing = true;
        handleAddAPIParam.setText("应用");
        deleteAPIButton.setVisible(!isEditing);
        APIListView.setLayoutY(APIListView.getLayoutY()+deleteAPIButton.getHeight());
        String selectedApi = APIListView.getSelectionModel().getSelectedItem();
        Map<String, String> params = parseURLParams(selectedApi);
        handleDebug("Params: " + params);
        if (selectedApi != null) {
            apiTextField.setText(selectedApi);
            apiTextField.setEditable(false);
        }
        APIListView.getColumns().remove(apiColumn);


        // 创建列
        TableColumn<String, String> Params = new TableColumn<>("参数名称（可能已在原始URL中定义）");
        TableColumn<String, String> Values = new TableColumn<>("参数值");

        Params.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().split("=")[0]));
        Values.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().split("=")[1]));

        APIListView.getColumns().add(Params);
        APIListView.getColumns().add(Values);

        // 将参数数据添加到表中
        ObservableList<String> paramList = FXCollections.observableArrayList();
        params.forEach((key, value) -> paramList.add(key + "=" + value));
        APIListView.setItems(paramList);

        // 创建一个新的TableColumn用于显示删除按钮
        TableColumn<String, Void> EditColumn = new TableColumn<>("操作");

        // 使用setCellFactory方法为列设置单元格工厂
        EditColumn.setCellFactory(param -> new TableCell<>() {
            private final MFXButton editButton = new MFXButton("编辑");
            private final MFXButton deleteButton = new MFXButton("删除");
            private final MFXButton addButton = new MFXButton("新增");
            private final HBox hbox = new HBox(editButton, deleteButton, addButton); //创建一个HBox以容纳按钮
            final MFXButton saveButton = new MFXButton("保存");
            TextField textField = new TextField();

            {
                editButton.setOnAction(event -> {
                    String selectedParam = getTableView().getItems().get(getIndex());
                    hbox.getChildren().clear();

                    textField.setPromptText("Example=xxx");
                    textField.setText(selectedParam);
                    textField.setPrefWidth(90);

                    hbox.getChildren().add(textField);
                    hbox.getChildren().add(saveButton);
                });
            }

            {
                saveButton.setOnAction(event -> {
                    try {
                        String selectedParam = getTableView().getItems().get(getIndex());
                        String newParam = textField.getText();

                        hbox.getChildren().clear();

                        getTableView().getItems().remove(selectedParam);
                        getTableView().getItems().add(newParam);

                        apiTextField.setText(ApiParamHandler.editParam(selectedParam, newParam, apiTextField.getText(), apiObservableList));
                        updateApiList();

                        getTableView().refresh(); // 刷新表格视图

                        hbox.getChildren().clear();
                        hbox.getChildren().addAll(editButton, deleteButton, addButton);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            {
                deleteButton.setOnAction(event -> {
                    String selectedParam = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(selectedParam);
                    apiTextField.setText(ApiParamHandler.deleteParam(selectedParam, apiTextField.getText(), apiObservableList));
                    updateApiList();

                    hbox.getChildren().clear();
                    hbox.getChildren().addAll(editButton, deleteButton, addButton);
                    getTableView().refresh(); // 刷新表格视图
                });
            }

            {
                addButton.setOnAction(_ -> {
                    hbox.getChildren().clear();

                    MFXButton saveButton = new MFXButton("保存");
                    TextField keyField = new TextField();

                    keyField.setPromptText("Example=xxx");
                    keyField.setPrefWidth(90);

                    saveButton.setOnAction(_ -> {
                        try {
                            String newParam = keyField.getText().trim();
                            getTableView().getItems().add(newParam);
                            apiTextField.setText(ApiParamHandler.addParam(newParam, apiTextField.getText(), apiObservableList));

                            updateApiList();
                            hbox.getChildren().clear();
                            hbox.getChildren().addAll(editButton, deleteButton, addButton);
                            getTableView().refresh(); // 刷新表格视图
                        } catch (Exception e) {
                            ExceptionForwarder.handleException(e);
                        }
                    });
                    hbox.getChildren().addAll(keyField, saveButton);
                    setGraphic(hbox);
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

        // 将按钮列添加到APIListView中
        APIListView.getColumns().add(EditColumn);
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
            ExceptionForwarder.handleException(e);
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
                                }
                            } else {
                                successCount++;
                            }
                        } else {
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