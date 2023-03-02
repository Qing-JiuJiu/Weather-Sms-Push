package com.xinqi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.utils.HttpsClientUtil;
import org.slf4j.Logger;


/**
 * @author XinQi
 */
public class ChatGPTApi {

    public static String getMessage(String chatGptApi, String message, Logger logger) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        String jsonData = "{\"model\": \"gpt-3.5-turbo\",\"messages\": [{\"role\": \"user\", \"content\": \"" + message + "\"}]}";

        logger.info("正在调用 ChatGPT API 获取聊天内容，请求地址: {}，请求参数: {}", url, jsonData);

        byte[] response = HttpsClientUtil.httpsPostChatGpt(url, jsonData, chatGptApi);

        //解析返回的json数据
        JsonNode jsonNode = new ObjectMapper().readTree(response);

        logger.info("调用 ChatGPT API 获取聊天内容返回结果: {}", jsonNode);

        //返回消息内容
        return jsonNode.get("choices").get(0).get("message").get("content").asText();
    }
}
