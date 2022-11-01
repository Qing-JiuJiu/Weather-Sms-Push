package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.utils.HttpsClientUtil;
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
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        logger.info("正在调用微信接口API获取微信Token，请求地址：" + url);
        byte[] response = HttpsClientUtil.httpsGet(url);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用微信接口API获取微信Token返回结果：{}", jsonNode);
        return jsonNode;
    }

    /**
     * 发送微信Api
     */
    public static void sendWeChat(String token, String data, Logger logger) throws Exception {
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token;
        logger.info("正在准备调用微信接口API发送公众号信息，微信所需的编码格式为ISO_8859_1，日志打印的编码格式为UTF-8，请注意区分。");
        logger.info("正在调用微信接口API发送公众号信息，请求地址：" + url + "，请求参数：" + data);
        byte[] response = HttpsClientUtil.httpsPost(url,new String(data.getBytes(), StandardCharsets.ISO_8859_1));
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用微信接口API发送公众号信息返回结果：{}", jsonNode);
    }

}
