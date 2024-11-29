package org.fireworkrocket.lookup.ui.exception;

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
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;

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
        if (openDialogs.size() >= 20) {
            ExceptionHandler.handleFatal("!!!异常量>20!!!", new Exception());
            return null;
        }

        AtomicReference<String> result = new AtomicReference<>(null);
        AtomicBoolean onTop = new AtomicBoolean(false);

        MFXGenericDialog dialog = MFXGenericDialogBuilder.build()
                .setHeaderText(title)
                .setContentText(message)
                .get();

        dialog.setOnClose(event -> {
            Stage stage = (Stage) dialog.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        });

        dialog.setOnMinimize(event -> {
            Stage stage = (Stage) dialog.getScene().getWindow();
            if (stage != null) {
                stage.setIconified(true);
            }
        });

        dialog.setOnAlwaysOnTop(event -> {
            onTop.set(!onTop.get());
            dialog.setAlwaysOnTop(onTop.get());
        });

        for (Map.Entry<String, Runnable> entry : buttons.entrySet()) {
            MFXButton button = new MFXButton(entry.getKey());
            button.setOnAction(event -> {
                Stage stage = (Stage) button.getScene().getWindow();
                if (stage != null) {
                    stage.close();
                }
                result.set(entry.getKey());
                if (entry.getValue() != null) {
                    entry.getValue().run();
                }
            });
            dialog.addActions(button);
        }

        if (DialogType.ERROR.equals(type)) {
            Platform.runLater(() -> {
                MFXButton closeAllButton = new MFXButton("关闭全部");
                closeAllButton.setOnAction(event -> closeOpenDialogs());
                dialog.addActions(closeAllButton);
            });
        }

        VBox vbox = new VBox(dialog);
        Scene scene = new Scene(vbox);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        openDialogs.add(new WeakReference<>(stage));
        Platform.runLater(stage::showAndWait);

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