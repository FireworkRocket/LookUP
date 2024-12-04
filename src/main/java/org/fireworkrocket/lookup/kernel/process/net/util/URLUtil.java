package org.fireworkrocket.lookup.kernel.process.net.util;

import java.util.HashMap;
import java.util.Map;

/**
 * URL 工具类，用于构建和解析 URL 参数。
 */
public class URLUtil {

    /**
     * 构建带有参数的 URL。
     *
     * @param baseURL 基础 URL
     * @param params 参数映射
     * @return 带有参数的 URL
     */
    public static String buildURLWithParams(String baseURL, Map<String, String> params) {
        StringBuilder urlWithParams = new StringBuilder(baseURL);
        if (params != null && !params.isEmpty()) {
            urlWithParams.append("?");
            params.forEach((key, value) -> urlWithParams.append(key).append("=").append(value).append("&"));
            urlWithParams.deleteCharAt(urlWithParams.length() - 1); // 删除最后一个&
        }
        return urlWithParams.toString();
    }

    /**
     * 解析 URL 参数。
     *
     * @param url 要解析的 URL
     * @return 参数映射
     */
    public static Map<String, String> parseURLParams(String url) {
        Map<String, String> params = new HashMap<>();
        if (url == null || url.isEmpty()) {
            return params;
        }
        if (url.contains("?")) {
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String[] paramPairs = urlParts[1].split("&");
                for (String pair : paramPairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length > 1) {
                        params.put(keyValue[0], keyValue[1]);
                    } else {
                        params.put(keyValue[0], "");
                    }
                }
            }
        }
        return params;
    }

    /**
     * 编辑 URL 参数。
     *
     * @param url 要编辑的 URL
     * @param key 要编辑的参数键
     * @param newValue 新值
     * @return 编辑后的 URL
     */
    public static String editURLParam(String url, String key, String newValue) {
        Map<String, String> params = parseURLParams(url);
        if (params.containsKey(key)) {
            params.put(key, newValue);
        }
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    /**
     * 替换 URL 参数。
     *
     * @param old 旧参数
     * @param url 要替换的 URL
     * @param key 参数键
     * @param newValue 新值
     * @return 替换后的 URL
     */
    public static String replaceURLParam(String old, String url, String key, String newValue) {
        return addURLParam(removeURLParam(url, old), key, newValue);
    }

    /**
     * 添加 URL 参数。
     *
     * @param url 要添加参数的 URL
     * @param key 参数键
     * @param value 参数值
     * @return 添加参数后的 URL
     */
    public static String addURLParam(String url, String key, String value) {
        Map<String, String> params = parseURLParams(url);
        params.put(key, value);
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    /**
     * 删除 URL 参数。
     *
     * @param url 要删除参数的 URL
     * @param key 参数键
     * @return 删除参数后的 URL
     */
    public static String removeURLParam(String url, String key) {
        Map<String, String> params = parseURLParams(url);
        params.remove(key);
        if (params.isEmpty()) {
            return url.split("\\?")[0]; // 如果没有参数，返回基础 URL
        }
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    /**
     * 删除所有 URL 参数。
     *
     * @param url 要删除参数的 URL
     * @return 删除参数后的基础 URL
     */
    public static String removeAllURLParams(String url) {
        return url.split("\\?")[0]; // 返回基础 URL
    }
}