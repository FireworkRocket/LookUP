package org.fireworkrocket.lookup.kernel.json_configuration;

import org.fireworkrocket.lookup.kernel.json_configuration.image.JsonData;

/**
 * JsonData 工厂接口，用于创建 JsonData 实例。
 */
public interface JsonDataFactory {
    JsonData createJsonData();
}