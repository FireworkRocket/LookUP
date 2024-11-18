package org.fireworkrocket.lookup.processor.JSON_Read_Configuration;

import java.util.List;

public interface JsonDataInterpreter {
    String getStatus(JsonData jsonData);
    List<JsonData.Data> getData(JsonData jsonData);
    String getUrl(JsonData jsonData);
}