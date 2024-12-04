package org.fireworkrocket.lookup.kernel.process;

import javafx.collections.ObservableList;
import org.fireworkrocket.lookup.kernel.config.DatabaseUtil;

import java.util.Map;

import static org.fireworkrocket.lookup.kernel.process.net.util.URLUtil.*;

/**
 * API 参数处理器，用于编辑、删除和添加 URL 参数。
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * ObservableList<String> apiObservableList = ...;
 * String apiUrl = "http://example.com/api?param1=value1&param2=value2";
 *
 * // 编辑参数
 * String newUrl = ApiParamHandler.editParam("param1=value1", "param1=newValue", apiUrl, apiObservableList);
 * System.out.println(newUrl); // 输出: http://example.com/api?param1=newValue&param2=value2
 *
 * // 删除参数
 * newUrl = ApiParamHandler.deleteParam("param2=value2", apiUrl, apiObservableList);
 * System.out.println(newUrl); // 输出: http://example.com/api?param1=value1
 *
 * // 添加参数
 * newUrl = ApiParamHandler.addParam("param3=value3", apiUrl, apiObservableList);
 * System.out.println(newUrl); // 输出: http://example.com/api?param1=value1&param2=value2&param3=value3
 * }</pre>
 */
public class ApiParamHandler {

    /**
     * 编辑指定 URL 的参数。
     *
     * @param selectedParam 选中的参数
     * @param newParam 新参数
     * @param apiUrl 要编辑的 URL
     * @param apiObservableList 可观察的 URL 列表
     * @return 编辑后的新 URL
     * @throws Exception 如果参数格式错误
     */
    public static String editParam(String selectedParam, String newParam, String apiUrl, ObservableList<String> apiObservableList) throws Exception {
        String[] keyValue = newParam.split("=");
        if (keyValue.length != 2) {
            throw new Exception("参数格式错误");
        }

        String newUrl = replaceURLParam(keyValue[0], apiUrl, keyValue[0], keyValue[1]);
        apiObservableList.remove(apiUrl);
        apiObservableList.add(newUrl);
        DatabaseUtil.replaceItem(apiUrl, newUrl);
        System.out.println("editParam: " + newUrl);
        return newUrl;
    }

    /**
     * 删除指定 URL 的参数。
     *
     * @param selectedParam 选中的参数
     * @param apiUrl 要删除参数的 URL
     * @param apiObservableList 可观察的 URL 列表
     * @return 删除参数后的新 URL
     */
    public static String deleteParam(String selectedParam, String apiUrl, ObservableList<String> apiObservableList) {
        String newUrl = removeURLParam(apiUrl, selectedParam.split("=")[0]);
        apiObservableList.remove(apiUrl);
        apiObservableList.add(newUrl);
        DatabaseUtil.replaceItem(apiUrl, newUrl);
        return newUrl;
    }

    /**
     * 添加参数到指定 URL。
     *
     * @param newParam 新参数
     * @param apiUrl 要添加参数的 URL
     * @param apiObservableList 可观察的 URL 列表
     * @return 添加参数后的新 URL
     * @throws Exception 如果参数格式错误或参数已存在
     */
    public static String addParam(String newParam, String apiUrl, ObservableList<String> apiObservableList) throws Exception {
        String[] keyValue = newParam.split("=");
        if (keyValue.length == 2) {
            String key = keyValue[0];
            String value = keyValue[1];
            Map<String, String> params = parseURLParams(apiUrl);
            if (!params.containsKey(key)) {
                String newUrl = addURLParam(apiUrl, key, value);
                apiObservableList.remove(apiUrl);
                apiObservableList.add(newUrl);
                DatabaseUtil.replaceItem(apiUrl, newUrl);
                return newUrl;
            } else {
                throw new Exception("参数已存在");
            }
        } else {
            throw new Exception("参数格式错误");
        }
    }
}