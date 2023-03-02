package com.xinqi.job;

import com.xinqi.api.ChatGPTApi;
import com.xinqi.api.TelegramBotApi;
import com.xinqi.utils.ProjectUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author XinQi
 */
public class ChatGPTJob implements Job {

    static Logger logger = LoggerFactory.getLogger(ChatGPTJob.class.getName());

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        //得到配置文件路径
        String configPath = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("configPath");

        //读取配置文件，获得相关参数
        Map<String, Object> config = ProjectUtils.readYamlConfig(configPath, logger);
        String telegramBotToken = (String)config.get("telegram_bot_token");

        //得到消息
        Map<String,String> botMessage;
        try {
            botMessage = TelegramBotApi.getUpdates(telegramBotToken,logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //如果没有新消息，直接返回
        if (botMessage == null) {
            return;
        }

        //得到回复
        String chatGptApi = (String) config.get("chatgpt_api");
        String chatGptMessage;
        try {
            chatGptMessage = ChatGPTApi.getMessage(chatGptApi, botMessage.get("message"), logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //发送消息
        try {
            TelegramBotApi.sendMessage(telegramBotToken, botMessage.get("chat_id"), chatGptMessage, logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
