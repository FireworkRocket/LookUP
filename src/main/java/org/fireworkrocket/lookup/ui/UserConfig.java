package org.fireworkrocket.lookup.ui;

import org.fireworkrocket.lookup.kernel.config.DefaultConfig;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * UserConfig 类，用于管理用户配置。
 */
public class UserConfig {

    private static final String CONFIG_FILE_PATH = DefaultConfig.configHome.getPath(); // 文件路径
    private final Properties properties;

    /**
     * 构造函数，读取配置文件。
     *
     * @throws IOException 如果读取文件失败
     */
    public UserConfig() throws IOException {
        properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
            properties.load(input);
        }
    }

    /**
     * 读取配置文件中的键值。
     *
     * @param key 键
     * @return 值
     */
    public String readProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * 写入配置文件。
     *
     * @param key 键
     * @param value 值
     * @throws IOException 如果写入文件失败
     */
    public void writeProperty(String key, String value) throws IOException {
        properties.setProperty(key, value);
        saveProperties();
    }

    /**
     * 编辑配置文件。
     *
     * @param key 键
     * @param newValue 新值
     * @throws IOException 如果编辑文件失败
     */
    public void editProperty(String key, String newValue) throws IOException {
        if (properties.containsKey(key)) {
            properties.setProperty(key, newValue);
            saveProperties();
        }
    }

    /**
     * 清空配置文件。
     *
     * @throws IOException 如果清空文件失败
     */
    public void clearProperties() throws IOException {
        properties.clear();
        saveProperties();
    }

    /**
     * 删除配置文件中的键值。
     *
     * @param key 键
     * @throws IOException 如果删除文件失败
     */
    public void removeProperty(String key) throws IOException {
        properties.remove(key);
        saveProperties();
    }

    /**
     * 读取配置文件中的多个键值。
     *
     * @param keys 键数组
     * @return 键值对
     */
    public Map<String, String> readProperties(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, properties.getProperty(key));
        }
        return result;
    }

    /**
     * 写入配置文件中的多个键值。
     *
     * @param entries 键值对
     * @throws IOException 如果写入文件失败
     */
    public void writeProperties(Map<String, String> entries) throws IOException {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        saveProperties();
    }

    /**
     * 保存配置文件。
     *
     * @throws IOException 如果保存文件失败
     */
    private void saveProperties() throws IOException {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE_PATH)) {
            properties.store(output, null);
        }
    }
}