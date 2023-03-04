package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.util.HttpsClientUtil;
import com.xinqi.util.ProjectUtils;
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
        //准备请求地址
        String url = "https://geoapi.qweather.com/v2/city/lookup?key=" + weatherKey + "&location=" + URLEncoder.encode(regionName, "UTF8");
        logger.info("正在调用和风天气获取地区 ID，请求地址: {}", url);

        //发送请求
        byte[] response = HttpsClientUtil.httpsGet(url);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(ProjectUtils.gzipDecompress(response));
        logger.info("调用和风天气获取地区 ID 返回结果: {}", jsonNode);

        //返回响应内容
        return jsonNode;
    }

    /**
     * 得到地区天气信息
     */
    public static JsonNode getWeather(String weatherKey, String regionId, Logger logger) throws Exception {
        //准备请求地址
        String url = "https://devapi.qweather.com/v7/weather/3d?location=" + regionId + "&key=" + weatherKey;
        logger.info("正在调用和风天气天气获取 API 获取天气信息，请求地址: {}", url);

        //发送请求
        byte[] response = HttpsClientUtil.httpsGet(url);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(ProjectUtils.gzipDecompress(response));
        logger.info("调用和风天气天气获取 API 获取天气信息返回结果: {}", jsonNode);

        //返回响应内容
        return jsonNode;
    }
}
