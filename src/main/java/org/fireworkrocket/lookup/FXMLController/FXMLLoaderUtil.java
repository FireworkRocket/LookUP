package org.fireworkrocket.lookup.FXMLController;

import javafx.application.Platform;
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
    private static final Map<String, WeakReference<Node>> fxmlCache = new LinkedHashMap<String, WeakReference<Node>>() {
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
        Platform.runLater(() -> {
            try {
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
                anchorPane.getChildren().clear();
                anchorPane.getChildren().add(node);
                AnchorPane.setTopAnchor(node, 0.0);
                AnchorPane.setBottomAnchor(node, 0.0);
                AnchorPane.setLeftAnchor(node, 0.0);
                AnchorPane.setRightAnchor(node, 0.0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}