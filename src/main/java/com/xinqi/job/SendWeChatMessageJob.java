package com.xinqi.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinqi.api.PoetryApi;
import com.xinqi.api.WeChatApi;
import com.xinqi.api.WeatherApi;
import com.xinqi.utils.ProjectUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SendWeChatMessageJob implements Job {

    static Logger logger = LoggerFactory.getLogger(SendWeChatMessageJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        logger.info("开始执行发送每日天气微信消息");

        //得到配置文件路径
        String configPath = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("configPath");

        //读取配置文件，获得相关参数
        Map<String, Object> config = ProjectUtils.readYamlConfig(configPath, logger);
        //和风天气私钥
        String weatherKey = (String) config.get("weather_key");
        //地区
        String region = (String) config.get("region");
        //微信相关参数
        String appId = (String) config.get("app_id");
        String appSecret = (String) config.get("app_secret");
        String templateId = (String) config.get("template_id");
        @SuppressWarnings("unchecked") List<String> receiveUserList = (List<String>) config.get("receive_user");

        //拿到微信token
        JsonNode jsonNode;
        try {
            jsonNode = WeChatApi.getToken(appId, appSecret, logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String accessToken = jsonNode.get("access_token").asText();

        //获得诗词
        try {
            jsonNode = PoetryApi.getPoetry(logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //得到诗词
        String content = jsonNode.get("content").asText();
        String origin = jsonNode.get("origin").asText();
        String author = jsonNode.get("author").asText();

        //获得和风天气地区代码和名字
        //先判断配置文件里是否存在天气地区代码，如果存在直接使用，减少Api调用次数
        String regionId = (String) config.get("regionId");
        String regionName = (String) config.get("regionName");
        //如果地区代码不存在，调用API获得地区代码
        if (regionId == null) {
            //获取新的地区代码
            try {
                logger.info("未在配置文件检测到regionId，正在通过配置文件调用和风天气地区ID获取API获取regionId，获取地区：" + region);
                jsonNode = WeatherApi.getRegionId(weatherKey, region, logger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            jsonNode = jsonNode.get("location").get(0);
            regionName = jsonNode.get("name").asText();
            regionId = jsonNode.get("id").asText();

            //替换region和regionId
            //判断有没有留空的最后一行，如果没有新建一行空的，用于写入正确的Yaml数据，因为测试到snakeyaml用map追加数据，如果map只追加一行数据不会自动换行，原因不明
            //冷门写法，流会在结束后自动close，采用rw读写模式
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(configPath, "rw")) {
                //将指针移动到最后一行前两个字节，因为一个换行占两个字节，返回到空格前面读取下一个字节判断是否是空格字节
                randomAccessFile.seek(randomAccessFile.length() - 2);
                //如果读到的最后面两个字节码是13回车，然后是10换行/新行，那么就是新行，不是就写入新的行
                if (randomAccessFile.read() != '\r' && randomAccessFile.read() != '\n') {
                    randomAccessFile.write(System.getProperty("line.separator").getBytes());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //将地区ID写入配置文件
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            FileWriter writer;
            try {
                writer = new FileWriter(configPath, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BufferedWriter buffer = new BufferedWriter(writer);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("regionName", regionName);
            map.put("regionId", regionId);
            yaml.dump(map, buffer);
            try {
                buffer.close();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("通过配置文件region:" + region + "调用和风天气地区ID获取API得到" + regionName + "的regionId:" + regionId + "，已将regionName、regionId写入配置文件，请确保regionName与region地区一致");
        } else {
            logger.info("已从配置文件读取到regionId，将直接使用regionId：" + regionId + "，若需要修改region，请删除已将regionName、regionId整行，否则获取的还是旧地区天气数据");
        }

        //获得当天天气信息
        try {
            jsonNode = WeatherApi.getWeather(weatherKey, regionId, logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //data数据
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

        //处理相关数据准备发送
        //日期 + 星期几
        String week = ProjectUtils.getDateWeekTime(fxDate);
        fxDate = fxDate + " " + week;
        //最低气温-最高气温
        String temp = tempMin + "℃ - " + tempMax + "℃";
        //诗词、名字、作者拼接
        String poetry = content + "——" + author + "《" + origin + "》";

        //发送微信消息
        //向所有用户发送微信消息
        String finalFxDate = fxDate;
        String finalRegionName = regionName;
        receiveUserList.forEach(receiveUser -> {
            //构建微信发送消息JSON
            String weChatJson = "{\"touser\":\"" + receiveUser + "\"," + "\"template_id\":\"" + templateId + "\"," + "\"data\":{\"date\":{\"value\":\"" + finalFxDate + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"region\":{\"value\":\"" + finalRegionName + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"weather\":{\"value\":\"" + textDay + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"temp\":{\"value\":\"" + temp + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"humidity\":{\"value\":\"" + humidity + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"precip\":{\"value\":\"" + precip + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"windScaleNight\":{\"value\":\"" + windScaleNight + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}," + "\"poetry\":{\"value\":\"" + poetry + "\",\"color\":\"" + ProjectUtils.getColor() + "\"}}}";
            try {
                //发送微信消息
                WeChatApi.sendWeChat(accessToken, weChatJson, logger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        logger.info("执行发送每日天气微信消息结束");
    }
}
