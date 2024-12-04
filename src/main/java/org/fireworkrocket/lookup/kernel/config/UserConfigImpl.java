package org.fireworkrocket.lookup.kernel.config;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UserConfigImpl implements UserConfig {

    private static final String CONFIG_FILE_PATH = DefaultConfig.configHome.getPath() + "//Software_Config.cfg";
    private final Properties properties;

    public UserConfigImpl() throws IOException {
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
        } else {
            updateDefaultConfigFromProperties();
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

    private void updateDefaultConfigFromProperties() {
        for (Field field : DefaultConfig.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String propertyValue = properties.getProperty(field.getName());
                if (propertyValue != null) {
                    if (field.getType().equals(int.class)) {
                        field.setInt(null, Integer.parseInt(propertyValue));
                    } else if (field.getType().equals(boolean.class)) {
                        field.setBoolean(null, Boolean.parseBoolean(propertyValue));
                    } else if (field.getType().equals(File.class)) {
                        field.set(null, new File(propertyValue));
                    } else {
                        field.set(null, propertyValue);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
    }

    @Override
    public String readProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public void writeProperty(String key, String value) throws IOException {
        if (key == null || key.isEmpty() || value == null) {
            throw new IllegalArgumentException("Key and value must not be null or empty");
        }
        properties.setProperty(key, value);
        saveProperties();
    }

    @Override
    public void editProperty(String key, String newValue) throws IOException {
        if (key == null || key.isEmpty() || newValue == null) {
            throw new IllegalArgumentException("Key and new value must not be null or empty");
        }
        if (properties.containsKey(key)) {
            properties.setProperty(key, newValue);
            saveProperties();
        }
    }

    @Override
    public void clearProperties() throws IOException {
        properties.clear();
        saveProperties();
    }

    @Override
    public void removeProperty(String key) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be null or empty");
        }
        properties.remove(key);
        saveProperties();
    }

    @Override
    public Map<String, String> readProperties(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, properties.getProperty(key));
        }
        return result;
    }

    @Override
    public void writeProperties(Map<String, String> entries) throws IOException {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Entries must not be null or empty");
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                throw new IllegalArgumentException("Key and value in entries must not be null or empty");
            }
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

