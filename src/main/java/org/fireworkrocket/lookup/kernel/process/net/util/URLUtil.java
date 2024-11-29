package org.fireworkrocket.lookup.kernel.process.net.util;

import java.util.HashMap;
import java.util.Map;

public class URLUtil {

    public static String buildURLWithParams(String baseURL, Map<String, String> params) {
        StringBuilder urlWithParams = new StringBuilder(baseURL);
        if (params != null && !params.isEmpty()) {
            urlWithParams.append("?");
            params.forEach((key, value) -> urlWithParams.append(key).append("=").append(value).append("&"));
            urlWithParams.deleteCharAt(urlWithParams.length() - 1); // 删除最后一个&
        }
        return urlWithParams.toString();
    }

    public static Map<String, String> parseURLParams(String url) { // 解析URL参数
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

    public static String editURLParam(String url, String key, String newValue) {
        Map<String, String> params = parseURLParams(url);
        if (params.containsKey(key)) {
            params.put(key, newValue);
        }
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    public static String replaceURLParam(String old, String url, String key, String newValue) {
        return addURLParam(removeURLParam(url,old), key, newValue);
    }

    public static String addURLParam(String url, String key, String value) {
        Map<String, String> params = parseURLParams(url);
        params.put(key, value);
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    public static String removeURLParam(String url, String key) {
        Map<String, String> params = parseURLParams(url);
        params.remove(key);
        if (params.isEmpty()) {
            return url.split("\\?")[0]; // 如果没有参数，返回基础URL
        }
        return buildURLWithParams(url.split("\\?")[0], params);
    }

    public static String removeAllURLParams(String url) {
        return url.split("\\?")[0]; // 返回基础URL
    }

}