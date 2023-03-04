package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.util.HttpsClientUtil;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

/**
 * @author XinQi
 */
public class WeChatApi {

    /**
     * 获得微信token
     */
    public static JsonNode getToken(String appId, String appSecret, Logger logger) throws Exception {
        //准备请求地址
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        logger.info("正在调用微信接口 API 获取微信 Token，请求地址: {}", url);

        //发送请求
        byte[] response = HttpsClientUtil.httpsGet(url);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用微信接口 API 获取微信 Token返回结果: {}", jsonNode);

        //返回响应内容
        return jsonNode;
    }

    /**
     * 发送微信Api
     */
    public static void sendWeChat(String token, String data, Logger logger) throws Exception {
        //准备请求地址
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token;
        logger.info("正在准备调用微信 API 发送公众号信息，微信所需的编码格式为ISO_8859_1，日志打印的编码格式为 UTF-8，请注意区分");
        logger.info("正在调用微信 API 发送公众号信息，请求地址: {}，请求参数: {}", url, data);

        //发送请求
        byte[] response = HttpsClientUtil.httpsPost(url, new String(data.getBytes(), StandardCharsets.ISO_8859_1));

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用微信 API 发送公众号信息返回结果: {}", jsonNode);
    }

}
