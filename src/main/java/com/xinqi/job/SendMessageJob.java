package com.xinqi.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinqi.api.SendSms;
import com.xinqi.utils.GzipUtills;
import com.xinqi.utils.HttpsClientUtil;
import com.xinqi.utils.ProjectUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

import java.util.List;
import java.util.Map;

/**
 * @author XinQi
 */
public class SendMessageJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Logger logger = LoggerFactory.getLogger("Job");
        String configPath = (String) jobExecutionContext.get("configPath");

        //读取配置文件，获得相关参数
        Map<String, Object> config = ProjectUtils.readConfig(logger, configPath);
        //和风天气私钥
        String weatherKey = (String) config.get("weather_key");
        //地区
        String region = (String) config.get("region");
        //SecretId
        String secretId = (String) config.get("SecretId");
        //SecretKey
        String secretKey = (String) config.get("SecretKey");
        //sdkAppId
        String sdkAppId = (String) config.get("sdkAppId");
        //signName
        String signName = (String) config.get("signName");
        //templateId
        String templateId = (String) config.get("templateId");
        //收件人列表
        List<String> addresseeList = (List<String>) config.get("addressee");
        addresseeList.forEach(addressee -> addresseeList.set(addresseeList.indexOf(addressee), "+86" + addressee));
        String[] addresseeArray = addresseeList.toArray(new String[0]);

        //获得今日好诗的诗词
        byte[] response;
        try {
            response = HttpsClientUtil.httpsGet("https://v1.jinrishici.com/all");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("古诗词API：" + jsonNode);
        String poetry = jsonNode.get("content").asText();

        //处理内容，因为短信模板一次性最多12个字符，分成两段，诗词比较少出现一段12个字
        String poetryPrefix;
        String poetrySuffix;
        String[] split = poetry.split("。", 2);
        //判断是否分割成功了，如果没有就分割其他符号
        if ("".equals(split[split.length - 1]) || split.length != 2) {
            split = poetry.split("？", 2);
            poetryPrefix = split[0] + "？";
            if ("".equals(split[split.length - 1]) || split.length != 2) {
                split = poetry.split("，");
                poetryPrefix = split[0] + "，";
            }
        } else {
            poetryPrefix = split[0] + "。";
        }
        //如果分割出了长度为3，有可能是4+4+7这种类型诗词
        if (split.length == 3) {
            poetryPrefix = split[0] + "，" + split[1] + "，";
            poetrySuffix = split[2];
            //如果其中一个长度超过12就调换拼接
            if (poetryPrefix.length()>12 || poetrySuffix.length()>12){
                poetryPrefix = split[0] + "，";
                poetrySuffix = split[1] + "，" + split[2];
            }
        } else {
            poetrySuffix = split[1];
        }

        //获得和风天气地区代码和名字
        try {
            response = HttpsClientUtil.httpsGet("https://geoapi.qweather.com/v2/city/lookup?key=" + weatherKey + "&location=" + URLEncoder.encode(region, "UTF8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            jsonNode = new ObjectMapper().readTree(GzipUtills.gzipDecompress(response));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("和风天气地区id获取API：" + jsonNode);
        jsonNode = jsonNode.get("location").get(0);
        String locationName = jsonNode.get("name").asText();
        String locationId = jsonNode.get("id").asText();

        //获得当天天气信息
        try {
            response = HttpsClientUtil.httpsGet("https://devapi.qweather.com/v7/weather/3d?location=" + locationId + "&key=" + weatherKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            jsonNode = new ObjectMapper().readTree(GzipUtills.gzipDecompress(response));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("和风天气天气获取API：" + jsonNode);
        jsonNode = jsonNode.get("daily").get(0);
        //日期
        String fxDate = jsonNode.get("fxDate").asText();
        //天气
        String textDay = jsonNode.get("textDay").asText();
        //湿度
        String humidity = jsonNode.get("humidity").asText();
        //最低气温
        String tempMin = jsonNode.get("tempMin").asText();
        //最高气温
        String tempMax = jsonNode.get("tempMax").asText();
        //降水量
        String precip = jsonNode.get("precip").asText();
        //风力等级
        String windScaleNight = jsonNode.get("windScaleDay").asText();

        //封装参数发送短信
        String[] parameter = {fxDate, locationName, textDay, humidity, tempMin + "℃ - " + tempMax + "℃", precip, windScaleNight, poetryPrefix, poetrySuffix};
        logger.info("腾讯云短信发送API：" + SendSms.sendSms(secretId, secretKey, sdkAppId, signName, templateId, addresseeArray, parameter));
    }
}
