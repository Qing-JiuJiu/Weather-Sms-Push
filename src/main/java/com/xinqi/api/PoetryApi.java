package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xinqi.util.HttpsClientUtil;
import org.slf4j.Logger;


/**
 * @author XinQi
 */
public class PoetryApi {

    /**
     * 调用用古诗词API
     */
    public static JsonNode getPoetry(Logger logger) throws Exception {
        //准备请求地址
        String url = "https://v1.jinrishici.com/all";
        logger.info("正在调用古诗词 API 获取古诗内容，请求地址: {}", url);

        //发送请求
        byte[] response = HttpsClientUtil.httpsGet(url);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用古诗词 API 获取古诗内容返回结果: {}", jsonNode);

        //返回响应内容
        return jsonNode;
    }
}
