package org.fireworkrocket.lookup.kernel.config;

import java.io.IOException;
import java.util.Map;

public interface UserConfig {
    String readProperty(String key);
    void writeProperty(String key, String value) throws IOException;
    void editProperty(String key, String newValue) throws IOException;
    void clearProperties() throws IOException;
    void removeProperty(String key) throws IOException;
    Map<String, String> readProperties(String... keys);
    void writeProperties(Map<String, String> entries) throws IOException;
}