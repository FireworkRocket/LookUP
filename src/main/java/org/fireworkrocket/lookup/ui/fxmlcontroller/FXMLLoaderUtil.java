package org.fireworkrocket.lookup.ui.fxmlcontroller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class FXMLLoaderUtil {

    private static final int CACHE_SIZE = 10; // 缓存大小
    private static final Map<String, WeakReference<Node>> fxmlCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, WeakReference<Node>> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public static void loadFXML(String resource, AnchorPane anchorPane) {
        HomeController.getInstance().setGoldProgress(-1);
        Task<Node> task = new Task<>() {
            @Override
            protected Node call() throws Exception {
                Node node = null;
                WeakReference<Node> weakRef = fxmlCache.get(resource);
                if (weakRef != null) {
                    node = weakRef.get();
                }
                if (node == null) {
                    FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderUtil.class.getResource(resource));
                    node = fxmlLoader.load();
                    fxmlCache.put(resource, new WeakReference<>(node));
                }
                return node;
            }
        };

        task.setOnSucceeded(event -> {
            Node node = task.getValue();
            Platform.runLater(() -> {
                anchorPane.getChildren().setAll(node); // 使用setAll替换clear和add
                AnchorPane.setTopAnchor(node, 0.0);
                AnchorPane.setBottomAnchor(node, 0.0);
                AnchorPane.setLeftAnchor(node, 0.0);
                AnchorPane.setRightAnchor(node, 0.0);
            });
            HomeController.getInstance().setGoldProgress(0);
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            throw new RuntimeException(e);
        });

        new Thread(task).start();
    }
}