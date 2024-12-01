package org.fireworkrocket.lookup.kernel.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class LoadConifg {
    //TODO 完成设置加载，并将设置界面完善
    private static final Map<String, Field> fieldMap = new HashMap<>();

    static {
        for (Field field : DefaultConfig.class.getDeclaredFields()) {
            field.setAccessible(true);
            fieldMap.put(field.getName(), field);
        }
    }

    public static void loadConfig() {
        try {
            UserConfig userConfig = new UserConfig();
            for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                String value = userConfig.readProperty(entry.getKey());
                if (value != null) {
                    Field field = entry.getValue();
                    try {
                        if (field.getType().equals(int.class)) {
                            field.setInt(null, Integer.parseInt(value));
                        } else if (field.getType().equals(boolean.class)) {
                            field.setBoolean(null, Boolean.parseBoolean(value));
                        } else if (field.getType().equals(File.class)) {
                            field.set(null, new File(value));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to set field value: " + field.getName(), e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveConfig() {
        try {
            UserConfig userConfig = new UserConfig();
            for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                Field field = entry.getValue();
                try {
                    Object value = field.get(null);
                    if (value != null) {
                        userConfig.writeProperty(entry.getKey(), value.toString());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to get field value: " + entry.getKey(), e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
}