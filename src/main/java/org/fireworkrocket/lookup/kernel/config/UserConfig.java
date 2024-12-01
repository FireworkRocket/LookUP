package org.fireworkrocket.lookup.kernel.config;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UserConfig {

    private static final String CONFIG_FILE_PATH = DefaultConfig.configHome.getPath() + "//Software_Config.cfg";
    private final Properties properties;

    public UserConfig() throws IOException {
        properties = new Properties();
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        }
        if (properties.isEmpty()) {
            writeDefaultConfig();
        }
    }

    private void writeDefaultConfig() throws IOException {
        for (Field field : DefaultConfig.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(null);
                if (value instanceof File) {
                    properties.setProperty(field.getName(), ((File) value).getPath());
                } else {
                    properties.setProperty(field.getName(), String.valueOf(value));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
        saveProperties();
    }

    public String readProperty(String key) {
        return properties.getProperty(key);
    }

    public void writeProperty(String key, String value) throws IOException {
        properties.setProperty(key, value);
        saveProperties();
    }

    public void editProperty(String key, String newValue) throws IOException {
        if (properties.containsKey(key)) {
            properties.setProperty(key, newValue);
            saveProperties();
        }
    }

    public void clearProperties() throws IOException {
        properties.clear();
        saveProperties();
    }

    public void removeProperty(String key) throws IOException {
        properties.remove(key);
        saveProperties();
    }

    public Map<String, String> readProperties(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, properties.getProperty(key));
        }
        return result;
    }

    public void writeProperties(Map<String, String> entries) throws IOException {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        saveProperties();
    }

    private void saveProperties() throws IOException {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE_PATH)) {
            properties.store(output, null);
        }
    }
}