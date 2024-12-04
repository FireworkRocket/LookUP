package org.fireworkrocket.lookup.kernel.json_configuration;

import com.google.gson.Gson;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;
import org.fireworkrocket.lookup.kernel.json_configuration.image.JsonData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JSON 数据处理器，用于从 URL 获取 JSON 数据并进行处理。
 */
public class JSON_Data_Processor {

    private static final Gson GSON = new Gson();
    private static final int BUFFER_SIZE = 8192;
    private static JsonDataFactory jsonDataFactory = new DefaultJsonDataFactory();
    private static Function<JsonData, Map<String, Object>> customProcessJsonData;
    private static BiFunction<JsonData.Data, Integer, Map<String, Object>> customPrintData;

    /**
     * 从指定 URL 获取 JSON 数据并进行处理。
     *
     * @param getUrl 要获取 JSON 数据的 URL
     * @return 处理后的数据映射
     * @throws Exception 如果处理过程中发生错误
     */
    public static Map<String, Object> getUrl(String getUrl) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        StringBuilder response = new StringBuilder(BUFFER_SIZE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openConnection(getUrl).getInputStream()), BUFFER_SIZE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (response.toString().trim().startsWith("{")) {
                JsonData jsonData = jsonDataFactory.createJsonData();
                jsonData = GSON.fromJson(response.toString(), jsonData.getClass());
                resultMap.putAll(processJsonData(jsonData));
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException("Error processing URL", ex);
        }
        return resultMap;
    }

    /**
     * 打开指定 URL 的连接。
     *
     * @param getUrl 要连接的 URL
     * @return 打开的 HttpURLConnection
     * @throws Exception 如果连接过程中发生错误
     */
    public static HttpURLConnection openConnection(String getUrl) throws Exception {
        URI uri = new URI(getUrl);
        URL url = uri.toURL();
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * 处理 JSON 数据。
     *
     * @param jsonData 要处理的 JSON 数据
     * @return 处理后的数据映射
     */
    private static Map<String, Object> processJsonData(JsonData jsonData) {
        if (customProcessJsonData != null) {
            return customProcessJsonData.apply(jsonData);
        }
        Map<String, Object> resultMap = new HashMap<>();
        if (jsonData.getStatus() != null) {
            resultMap.put("Status", jsonData.getStatus());
        }
        List<JsonData.Data> dataList = jsonData.getData();
        if (dataList != null) {
            int count = 0;
            for (JsonData.Data data : dataList) {
                resultMap.putAll(putData(data, count));
                count++;
            }
        } else {
            resultMap.put("URL", jsonData.getUrl());
        }
        return resultMap;
    }

    /**
     * put JSON 数据。
     *
     * @param data 要put的数据
     * @param count 数据计数
     * @return put后的数据映射
     */
    private static Map<String, Object> putData(JsonData.Data data, int count) {
        if (customPrintData != null) {
            return customPrintData.apply(data, count);
        }
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("$Data" + count, data);
        dataMap.put("URL", data.getOriginalUrl() == null ? data.getUrl() : data.getOriginalUrl());
        dataMap.put("PID", data.getPid());
        dataMap.put("Page", data.getPage());
        dataMap.put("Author", data.getAuthor());
        dataMap.put("Title", data.getTitle());
        dataMap.put("R18", data.getR18Vel());
        dataMap.put("Upload Date", data.getUploadDate());
        dataMap.put("Tags", data.getTags());
        dataMap.put("Ext", data.getExt());
        dataMap.put("Resolution", data.getRes());
        return dataMap;
    }

    /**
     * 设置自定义的 JsonData 工厂。
     *
     * @param factory 自定义的 JsonData 工厂
     */
    public static void setJsonDataFactory(JsonDataFactory factory) {
        jsonDataFactory = factory;
    }

    /**
     * 设置自定义的 JSON 数据处理函数。
     *
     * @param customProcessJsonData 自定义的 JSON 数据处理函数
     */
    public static void setCustomProcessJsonData(Function<JsonData, Map<String, Object>> customProcessJsonData) {
        JSON_Data_Processor.customProcessJsonData = customProcessJsonData;
    }

    /**
     * 设置自定义的 JSON 数据打印函数。
     *
     * @param customPrintData 自定义的 JSON 数据put函数
     */
    public static void setCustomPrintData(BiFunction<JsonData.Data, Integer, Map<String, Object>> customPrintData) {
        JSON_Data_Processor.customPrintData = customPrintData;
    }
}