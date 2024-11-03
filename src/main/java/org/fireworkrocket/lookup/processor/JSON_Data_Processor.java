package org.fireworkrocket.lookup.processor;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON_Data_Processor 类，用于处理 JSON 数据。
 */
public class JSON_Data_Processor {

    private static final Gson GSON = new Gson();
    private static final int BUFFER_SIZE = 8192; // 8 KB 缓冲区大小

    /**
     * 从指定的 URL 获取 JSON 数据并解析为 Map。
     *
     * @param getUrl 要获取数据的 URL
     * @return 包含解析数据的 Map
     * @throws Exception 如果处理 URL 时发生错误
     */
    public static Map<String, Object> getUrl(String getUrl) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder(BUFFER_SIZE);
        try {
            URI uri = new URI(getUrl);
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()), BUFFER_SIZE);
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (response.toString().trim().startsWith("{")) {
                JsonData jsonData = GSON.fromJson(response.toString(), JsonData.class);
                if (jsonData.getStatus() != null) {
                    resultMap.put("Status", jsonData.getStatus());
                }
                List<JsonData.Data> dataList = jsonData.getData();
                if (dataList != null) {
                    int count = 0;
                    for (JsonData.Data data : dataList) {
                        resultMap.putAll(printData(data, count));
                        count++;
                    }
                } else {
                    resultMap.put("URL", jsonData.getUrl());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Exception("Error processing URL", ex);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resultMap;
    }

    /**
     * 打印数据并返回包含数据的 Map。
     *
     * @param data 要打印的数据
     * @param count 数据计数
     * @return 包含数据的 Map
     */
    private static Map<String, Object> printData(JsonData.Data data, int count) {
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
}