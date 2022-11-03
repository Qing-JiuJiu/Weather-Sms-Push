package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.utils.GzipUtils;
import com.xinqi.utils.HttpsClientUtil;
import org.slf4j.Logger;

import java.net.URLEncoder;

/**
 * @author XinQi
 */
public class WeatherApi {

    /**
     * 得到地区ID
     */
    public static JsonNode getRegionId(String weatherKey, String regionName, Logger logger) throws Exception {
        String url = "https://geoapi.qweather.com/v2/city/lookup?key=" + weatherKey + "&location=" + URLEncoder.encode(regionName, "UTF8");
        logger.info("正在调用和风天气获取地区 ID 接口，请求地址: {}", url);
        byte[] response = HttpsClientUtil.httpsGet(url);
        JsonNode jsonNode = new ObjectMapper().readTree(GzipUtils.gzipDecompress(response));
        logger.info("调用和风天气获取地区 ID 接口返回结果: {}", jsonNode);
        return jsonNode;
    }

    /**
     * 得到地区天气信息
     */
    public static JsonNode getWeather(String weatherKey, String regionId, Logger logger) throws Exception {
        String url = "https://devapi.qweather.com/v7/weather/3d?location=" + regionId + "&key=" + weatherKey;
        logger.info("正在调用和风天气天气获取 API 获取天气信息，请求地址: {}", url);
        byte[] response = HttpsClientUtil.httpsGet(url);
        JsonNode jsonNode = new ObjectMapper().readTree(GzipUtils.gzipDecompress(response));
        logger.info("调用和风天气天气获取 API 获取天气信息返回结果: {}", jsonNode);
        return jsonNode;
    }
}
