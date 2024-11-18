package org.fireworkrocket.lookup.processor.JSON_Read_Configuration;

import com.google.gson.Gson;
import org.fireworkrocket.lookup.exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSON_Data_Processor {

    private static final Gson GSON = new Gson();
    private static final int BUFFER_SIZE = 8192;

    public static Map<String, Object> getUrl(String getUrl) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        StringBuilder response = new StringBuilder(BUFFER_SIZE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openConnection(getUrl).getInputStream()), BUFFER_SIZE)) {
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
            ExceptionHandler.handleException("Error processing URL", ex);
        }
        return resultMap;
    }

    public static HttpURLConnection openConnection(String getUrl) throws Exception {
        URI uri = new URI(getUrl);
        URL url = uri.toURL();
        return (HttpURLConnection) url.openConnection();
    }

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