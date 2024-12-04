package org.fireworkrocket.lookup.kernel.json_configuration;

import org.fireworkrocket.lookup.kernel.json_configuration.image.JsonData;

/**
 * 默认的 JsonData 工厂实现。
 */
public class DefaultJsonDataFactory implements JsonDataFactory {

    @Override
    public JsonData createJsonData() {
        return new JsonData();
    }
}