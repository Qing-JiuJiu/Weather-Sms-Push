package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.utils.HttpsClientUtil;
import org.slf4j.Logger;

import java.util.HashMap;

/**
 * @author XinQi
 */
public class TelegramBotApi {

    /**
     * 得到消息的偏移数据
     */
    static int update_id = 0;

    /**
     * @return 得到的消息
     */
    public static HashMap<String, String> getUpdates(String botApi, Logger logger) throws Exception {
        //请求接口
        String url = "https://api.telegram.org/bot" + botApi + "/getUpdates";
        String jsonData = "{\"offset\":" + update_id + "}";
        byte[] response = HttpsClientUtil.httpsPost(url, jsonData);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        JsonNode result = jsonNode.get("result");

        //判断是否收到消息
        if (result.size() == 0) {
            return null;
        }

        //将收到消息的第一个update_id设置为偏移量并且再次获取该重复消息以便将该消息偏移去除
        if (update_id == 0) {
            update_id = result.get(0).get("update_id").asInt();
            return getUpdates(botApi, logger);
        }

        logger.info("循环调用 TelegramBot API 得到新消息，请求地址: {}，接口返回内容为: {}，当前 update_id 为: {}", url, jsonNode, update_id);

        //更新偏移量
        update_id++;

        //返回两个内容，一个是消息内容，一个是消息发送者的id
        HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put("message", result.get(0).get("message").get("text").asText());
        responseMap.put("chat_id", result.get(0).get("message").get("from").get("id").asText());
        return responseMap;
    }

    public static void sendMessage(String botApi, String sendId, String message, Logger logger) throws Exception {
        //请求接口
        String url = "https://api.telegram.org/bot" + botApi + "/sendMessage";
        String jsonData = "{\n\"chat_id\": " + sendId + ",\n\"text\": \"" + message + "\"}";

        logger.info("正在调用 TelegramBot API 发送消息，请求地址: {}，请求参数: {}", url, jsonData);

        byte[] response = HttpsClientUtil.httpsPost(url, jsonData);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        logger.info("调用 TelegramBot API 发送消息返回结果:: {}", jsonNode);
    }
}
