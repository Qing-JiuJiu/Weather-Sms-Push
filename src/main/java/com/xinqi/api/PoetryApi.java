package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xinqi.utils.HttpsClientUtil;
import org.slf4j.Logger;


/**
 * @author XinQi
 */
public class PoetryApi {

    /**
     * 调用用古诗词API
     */
    public static JsonNode getPoetry(Logger logger) throws Exception {
        String url = "https://v1.jinrishici.com/all";
        logger.info("正在调用古诗词 API 获取古诗内容，请求地址: {}", url);
        byte[] response = HttpsClientUtil.httpsGet(url);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用古诗词 API 获取古诗内容返回结果: {}", jsonNode);
        return jsonNode;
    }
}
