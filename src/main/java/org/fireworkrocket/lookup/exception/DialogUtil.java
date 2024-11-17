package org.fireworkrocket.lookup.exception;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.enums.DialogType;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用于显示对话框的工具类。
 */
public class DialogUtil {

    private static final List<WeakReference<Stage>> openDialogs = new ArrayList<>();

    /**
     * 显示一个等待用户响应的对话框。
     *
     * @param type    对话框类型
     * @param title   对话框标题
     * @param message 对话框内容
     * @param buttons 按钮及其对应的操作
     * @return 用户点击的按钮文本
     */
    public static AtomicReference<String> showDialog(DialogType type, String title, String message, Map<String, Runnable> buttons) {
        if (openDialogs.size() >= 20){
            ExceptionHandler.handleFatal("!!!异常量>20!!!",new Exception());
            return null;
        }

        AtomicReference<String> result = new AtomicReference<>(null); // 初始化为null
        AtomicBoolean onTop = new AtomicBoolean(false);

        // 使用构建器配置对话框
        MFXGenericDialog dialog = MFXGenericDialogBuilder.build()
                .setHeaderText(title)
                .setContentText(message)
                .get();

        dialog.setOnClose(event -> {
            WeakReference<Stage> stageRef = new WeakReference<>((Stage) dialog.getScene().getWindow());
            Stage stage = stageRef.get();
            if (stage != null) {
                stage.close();
            }
        });

        dialog.setOnMinimize(event -> {
            WeakReference<Stage> stageRef = new WeakReference<>((Stage) dialog.getScene().getWindow());
            Stage stage = stageRef.get();
            if (stage != null) {
                stage.setIconified(true); // 最小化窗口
            }
        });

        dialog.setOnAlwaysOnTop(event -> {
            onTop.set(!onTop.get());
            dialog.setAlwaysOnTop(onTop.get());
        });

        for (Map.Entry<String, Runnable> entry : buttons.entrySet()) {
            MFXButton button = new MFXButton(entry.getKey());
            button.setOnAction(event -> { // 鼠标点击事件
                WeakReference<Stage> stageRef = new WeakReference<>((Stage) button.getScene().getWindow());
                Stage stage = stageRef.get();
                if (stage != null) {
                    stage.close();
                }
                result.set(entry.getKey()); // 设置返回值
                if (entry.getValue() != null) {
                    entry.getValue().run();
                }
            });
            dialog.addActions(button);
        }

        // 添加关闭全部按钮
        if (DialogType.ERROR.equals(type)) {
        Platform.runLater(()->{
            MFXButton closeAllButton = new MFXButton("关闭全部");
            closeAllButton.setOnAction(event -> closeOpenDialogs());
            dialog.addActions(closeAllButton);
        });
        }

        VBox vbox = new VBox(dialog);
        Scene scene = new Scene(vbox);
        WeakReference<Stage> stageRef = new WeakReference<>(new Stage());
        Stage stage = stageRef.get();
        if (stage != null) {
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED); // 设置无装饰窗口
            stage.setScene(scene);
            openDialogs.add(stageRef); // 添加到打开的弹窗列表
            stage.showAndWait();
        }

        return result;
    }

    private static void closeOpenDialogs() {
        for (WeakReference<Stage> stageRef : openDialogs) {
            Stage stage = stageRef.get();
            if (stage != null) {
                Platform.runLater(stage::close);
            }
        }
        openDialogs.clear();
    }
}