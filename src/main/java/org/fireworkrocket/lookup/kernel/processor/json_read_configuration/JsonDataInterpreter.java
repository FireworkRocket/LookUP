package org.fireworkrocket.lookup.kernel.processor.json_read_configuration;

import java.util.List;

public interface JsonDataInterpreter {
    String getStatus(JsonData jsonData);
    List<JsonData.Data> getData(JsonData jsonData);
    String getUrl(JsonData jsonData);
}