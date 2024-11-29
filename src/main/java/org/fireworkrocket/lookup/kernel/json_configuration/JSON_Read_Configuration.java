package org.fireworkrocket.lookup.kernel.json_configuration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fireworkrocket.lookup.kernel.config.DefaultConfig;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JSON 预定义模板加载器
 * @version 1.2.0
 * @see JsonDataInterpreter
 * @see DefaultJsonDataInterpreter
 * */
class JsonData {
    private String status;
    private String code;
    private String error;
    private String success;
    private List<JsonData.Data> data;
    private String url;
    private List<String> pics;
    private List<String> pic;
    private String imgurl;
    private String imageUrl;
    private String acgurl;
    private String file_url;

    private JsonDataInterpreter interpreter;

    public JsonData() {
        this.interpreter = new DefaultJsonDataInterpreter();
    }

    public void setInterpreter(JsonDataInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    String getStatus() {
        return interpreter.getStatus(this);
    }

    List<JsonData.Data> getData() {
        return interpreter.getData(this);
    }

    String getUrl() {
        return interpreter.getUrl(this);
    }

    static class Data {
        private String pid;
        private String page;
        private String author;
        private String title;
        private boolean r18;
        private String uploadDate;
        private List<String> tags;
        private Urls urls;
        private String url;
        private int width;
        private int height;
        private String ext;

        public String getPid() {
            return pid;
        }

        public String getPage() {
            return page;
        }

        public String getAuthor() {
            return author;
        }

        public boolean getR18Vel() {
            return r18;
        }

        public String getTitle() {
            return title;
        }

        public String getUploadDate() {
            return uploadDate;
        }

        public List<String> getTags() {
            return tags;
        }

        public String getOriginalUrl() {
            return urls != null ? urls.getOriginal() : null;
        }

        public String getRes() {
            return width + "x" + height;
        }

        public String getExt() {
            return ext;
        }

        public String getUrl() {
            return url;
        }

        static class Urls {
            private String original;

            public String getOriginal() {
                return original;
            }
        }
    }

    /**
     * LookUp JSON 默认模板
     * @version 1.2.0
     * @see JsonDataInterpreter
     * @see DefaultJsonDataInterpreter
     * */
    public class DefaultJsonDataInterpreter implements JsonDataInterpreter {
        private List<Map<String, String>> formats;
        private final ReentrantLock lock = new ReentrantLock();

        public DefaultJsonDataInterpreter() {
            loadFormats();
        }

        static boolean hasConfigFile = true;
        private void loadFormats() {
            if (hasConfigFile) {
                lock.lock();
                try {
                    File configFile = new File(DefaultConfig.configHome+"/json_formats.json").getAbsoluteFile();
                    try (FileReader reader = new FileReader(configFile)) {
                        Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                        formats = new Gson().fromJson(reader, listType);
                    } catch (IOException e) {
                        ExceptionHandler.handleWarning("Error loading JSON formats");
                        hasConfigFile = false;
                        formats = Collections.emptyList(); // 如果加载配置文件失败，formats 设为空列表
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public String getStatus(JsonData jsonData) {
            lock.lock();
            try {
                if (formats != null) {
                    for (Map<String, String> format : formats) {
                        String statusField = format.get("status");
                        try {
                            Field field = JsonData.class.getDeclaredField(statusField);
                            field.setAccessible(true);
                            Object value = field.get(jsonData);
                            if (value instanceof String && !((String) value).isEmpty()) {
                                return (String) value;
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            ExceptionHandler.handleException("Error getting status field", e);
                        }
                    }
                }
                return getDefaultStatus(jsonData);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public List<JsonData.Data> getData(JsonData jsonData) {
            lock.lock();
            try {
                if (formats != null) {
                    for (Map<String, String> format : formats) {
                        String dataField = format.get("data");
                        try {
                            Field field = JsonData.class.getDeclaredField(dataField);
                            field.setAccessible(true);
                            Object value = field.get(jsonData);
                            if (value instanceof List) {
                                return (List<JsonData.Data>) value;
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            ExceptionHandler.handleException("Error getting data field", e);
                        }
                    }
                }
                return jsonData.data;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public String getUrl(JsonData jsonData) {
            return getFieldValue(jsonData, "url").orElse(getDefaultUrl(jsonData));
        }

        private Optional<String> getFieldValue(JsonData jsonData, String fieldName) {
            lock.lock();
            try {
                if (formats != null) {
                    for (Map<String, String> format : formats) {
                        String field = format.get(fieldName);
                        try {
                            Field declaredField = JsonData.class.getDeclaredField(field);
                            declaredField.setAccessible(true);
                            Object value = declaredField.get(jsonData);
                            if (value instanceof String && !((String) value).isEmpty()) {
                                return Optional.of((String) value);
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            ExceptionHandler.handleException("Error getting " + fieldName + " field", e);
                        }
                    }
                }
                return Optional.empty();
            } finally {
                lock.unlock();
            }
        }

        private String getDefaultStatus(JsonData jsonData) {
            if (jsonData.status != null && !jsonData.status.isEmpty()) {
                return jsonData.status;
            } else if (jsonData.code != null && !jsonData.code.isEmpty()) {
                return jsonData.code;
            } else if (jsonData.error != null && !jsonData.error.isEmpty()) {
                return jsonData.error;
            } else if (jsonData.success != null && !jsonData.success.isEmpty()) {
                return jsonData.success;
            } else {
                return null;
            }
        }

        private String getDefaultUrl(JsonData jsonData) {
            if (jsonData.url != null && !jsonData.url.isEmpty()) {
                return jsonData.url;
            } else if (jsonData.pics != null && !jsonData.pics.isEmpty()) {
                return String.join(" $n ", jsonData.pics);
            } else if (jsonData.imgurl != null && !jsonData.imgurl.isEmpty()) {
                return jsonData.imgurl;
            } else if (jsonData.pic != null && !jsonData.pic.isEmpty()) {
                return String.join(" $n ", jsonData.pic);
            } else if (jsonData.imageUrl != null && !jsonData.imageUrl.isEmpty()) {
                return jsonData.imageUrl;
            } else if (jsonData.acgurl != null && !jsonData.acgurl.isEmpty()) {
                return jsonData.acgurl;
            } else if (jsonData.file_url != null && !jsonData.file_url.isEmpty()) {
                return jsonData.file_url;
            } else {
                for (JsonData.Data data : jsonData.getData()) {
                    if (data.getOriginalUrl() != null && !data.getOriginalUrl().isEmpty()) {
                        return data.getOriginalUrl();
                    } else if (data.getUrl() != null && !data.getUrl().isEmpty()) {
                        return data.getUrl();
                    }
                }
                return null;
            }
        }
    }
}