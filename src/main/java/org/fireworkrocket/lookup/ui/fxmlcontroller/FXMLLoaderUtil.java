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

/**
 * FXMLLoaderUtil 类，用于加载 FXML 文件并缓存节点。
 */
public class FXMLLoaderUtil {

    private static final int CACHE_SIZE = 10; // 缓存大小
    private static final Map<String, WeakReference<Node>> fxmlCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, WeakReference<Node>> eldest) {
            return size() > CACHE_SIZE; // 超过缓存大小时移除最旧的条目
        }
    };

    /**
     * 加载 FXML 文件并将其添加到 AnchorPane 中。
     *return size() > CACHE_SIZE; // 超过缓存大小时移除最旧的条目
     * @param resource FXML 文件的资源路径
     * @param anchorPane 要添加节点的 AnchorPane
     */
    public static void loadFXML(String resource, AnchorPane anchorPane) {
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
            anchorPane.getChildren().clear();
            anchorPane.getChildren().add(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            throw new RuntimeException(e);
        });

        new Thread(task).start();
    }
}