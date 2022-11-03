package com.xinqi.job;

import com.fasterxml.jackson.databind.JsonNode;

import com.xinqi.api.PoetryApi;
import com.xinqi.api.SendSmsApi;
import com.xinqi.api.WeChatApi;
import com.xinqi.api.WeatherApi;

import com.xinqi.utils.ProjectUtils;

import org.quartz.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;

import java.util.*;

/**
 * @author XinQi
 */

public class SendMessageJob implements Job{

    static Logger logger = LoggerFactory.getLogger(SendMessageJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        logger.info("开始执行每日天气推送");

        //得到配置文件路径
        String configPath = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("configPath");

        //读取配置文件，获得相关参数
        Map<String, Object> config = ProjectUtils.readYamlConfig(configPath, logger);
        //和风天气私钥
        String weatherKey = (String) config.get("weather_key");
        //地区
        String region = (String) config.get("region");

        //获得今日好诗的诗词字符串
        JsonNode poetryJsonNode;
        try {
            poetryJsonNode = PoetryApi.getPoetry(logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String content = poetryJsonNode.get("content").asText();

        //获得和风天气地区代码和名字
        //先判断配置文件里是否存在天气地区代码，如果存在直接使用，减少Api调用次数
        String regionId = (String) config.get("regionId");
        String regionName = (String) config.get("regionName");
        JsonNode jsonNode;
        //如果地区代码不存在，调用API获得地区代码
        if (regionId == null) {
            //获取新的地区代码
            try {
                logger.info("未在配置文件检测到 regionId，正在通过配置文件调用和风天气地区 ID 获取 API 获取 regionId，获取地区: " + region);
                jsonNode = WeatherApi.getRegionId(weatherKey, region ,logger);
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
            logger.info("通过配置文件 region:" + region + "调用和风天气地区 ID 获取 API 得到" + regionName + "的 regionId: " + regionId + "，已将 regionName 和 regionId 写入配置文件，请确保 regionName 与 region 地区一致");
        } else {
            logger.info("已从配置文件读取到 regionId，将直接使用 regionId: " + regionId + "，若需要修改 region，请删除已将 regionName 和 regionId 整行，否则获取的还是旧地区天气内容");
        }

        //获得当天天气信息
        try {
            jsonNode =WeatherApi.getWeather(weatherKey, regionId, logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        //处理相关参数
        //最低气温-最大气温
        String temp = tempMin + "℃ - " + tempMax + "℃";

        //判断是否发送短信
        if ((Boolean)config.get("sms_enable")) {
            //从配置文件获取相关腾讯云短信API参数
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
            //收件人列表，该注解解除List警告，主要是因为读取来自配置文件，一定会是List<String>
            @SuppressWarnings("unchecked") List<String> addresseeList = (List<String>) config.get("addressee");
            addresseeList.forEach(addressee -> addresseeList.set(addresseeList.indexOf(addressee), "+86" + addressee));
            String[] addresseeArray = addresseeList.toArray(new String[0]);

            //再次处理参数因腾讯云短信限制的参数
            //分割诗词，因为腾讯云一个参数最大长度为12
            String[] splitPoetry;
            try {
                splitPoetry = splitPoetry(content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //封装参数发送短信
            String[] parameter = {fxDate, regionName, textDay, humidity, temp, precip, windScaleNight, splitPoetry[0], splitPoetry[1]};
            SendSmsApi.sendSms(secretId, secretKey, sdkAppId, signName, templateId, addresseeArray, parameter, logger);
            logger.info(fxDate + "今日天气已推送至短信，若无收到短信，请检查各项 API 日志内容。");
        }

        //判断是否发送微信公众平台
        if ((Boolean)config.get("wechat_enable")) {
            //从配置文件获取微信相关参数
            String appId = (String) config.get("app_id");
            String appSecret = (String) config.get("app_secret");
            String templateId = (String) config.get("template_id");
            //关注公众号用户
            @SuppressWarnings("unchecked") List<String> receiveUserList = (List<String>) config.get("receive_user");

            //拿到微信Token
            try {
                jsonNode = WeChatApi.getToken(appId, appSecret, logger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String accessToken = jsonNode.get("access_token").asText();

            //处理相关内容
            //拼接诗词内容-作者-诗词名
            String origin = poetryJsonNode.get("origin").asText();
            String author = poetryJsonNode.get("author").asText();
            String poetry = content + "——" + author + "《" + origin + "》";
            //日期 + 星期几
            String week = ProjectUtils.getDateWeekTime(fxDate);
            fxDate = fxDate + " " + week;

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
            logger.info("今日天气已推送至微信公众平台，若微信没收到消息，请检查各项 API 日志内容。");
        }

        logger.info("执行每日天气推送结束");
    }

    /**
     * 获取随机一首诗词，并切分成前后两段
     */
    public static String[] splitPoetry(String poetry) throws Exception {
        //切分成前后两段
        //处理内容，因为短信模板一次性最多12个字符，分成前后两段，诗词比较少出现一段12个字
        String poetryPrefix = null;
        String poetrySuffix = null;
        List<Character> stringList = Arrays.asList('，', '。', '！', '？');
        boolean splitBoolean = true;
        //尝试分割"。"
        String[] split = poetry.split("。");
        if (split.length == 2 && !"".equals(split[split.length - 1])) {
            poetryPrefix = split[0] + "。";
            poetrySuffix = split[1];
            if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                poetrySuffix = poetrySuffix + "。";
            }
            if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                splitBoolean = false;
            }
        } else if (split.length == 3) {
            poetryPrefix = split[0] + "。" + split[1] + "。";
            poetrySuffix = split[2];
            if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                poetrySuffix = poetrySuffix + "。";
            }
            if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                poetryPrefix = split[0] + "。";
                poetrySuffix = split[1] + "。" + split[2];
                if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                    poetrySuffix = poetrySuffix + "。";
                }
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    splitBoolean = false;
                }
            }
        } else {
            splitBoolean = false;
        }
        //如果无法正确分割"，"，尝试分割"？"
        if (!splitBoolean) {
            split = poetry.split("？");
            if (split.length == 2 && !"".equals(split[split.length - 1])) {
                poetryPrefix = split[0] + "？";
                poetrySuffix = split[1];
                if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                    poetrySuffix = poetrySuffix + "？";
                }
                splitBoolean = poetryPrefix.length() <= 12 && poetrySuffix.length() <= 12;

            } else if (split.length == 3) {
                poetryPrefix = split[0] + "？" + split[1] + "？";
                poetrySuffix = split[2];
                if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                    poetrySuffix = poetrySuffix + "？";
                }
                splitBoolean = true;
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    poetryPrefix = split[0] + "？";
                    poetrySuffix = split[1] + "？" + split[2];
                    if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                        poetrySuffix = poetrySuffix + "？";
                    }
                    if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                        splitBoolean = false;
                    }
                }
            }
        }
        //如果无法正确分割"？"，尝试分割"！"
        if (!splitBoolean) {
            split = poetry.split("！");
            if (split.length == 2 && !"".equals(split[split.length - 1])) {
                poetryPrefix = split[0] + "！";
                poetrySuffix = split[1];
                if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                    poetrySuffix = poetrySuffix + "！";
                }
                splitBoolean = poetryPrefix.length() <= 12 && poetrySuffix.length() <= 12;

            } else if (split.length == 3) {
                poetryPrefix = split[0] + "？" + split[1] + "！";
                poetrySuffix = split[2];
                if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                    poetrySuffix = poetrySuffix + "！";
                }
                splitBoolean = true;
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    poetryPrefix = split[0] + "！";
                    poetrySuffix = split[1] + "！" + split[2];
                    if (!stringList.contains(poetrySuffix.charAt(poetrySuffix.length() - 1))) {
                        poetrySuffix = poetrySuffix + "！";
                    }
                    if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                        splitBoolean = false;
                    }
                }
            }
        }
        //如果无法正确分割"！"，尝试分割"，"，如果还是无法正确分割，将直接执行回调函数。
        if (!splitBoolean) {
            split = poetry.split("，");
            if (split.length == 2 && !"".equals(split[split.length - 1])) {
                poetryPrefix = split[0] + "，";
                poetrySuffix = split[1];
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    logger.warn("从古诗词 API 获取的诗词: \"" + poetry + "\"无法正确分割出两段，将重新调用古诗词 API 获取新的诗词");
                    poetry = PoetryApi.getPoetry(logger).get("content").asText();
                    splitPoetry(poetry);
                }
            } else if (split.length == 3) {
                poetryPrefix = split[0] + "，" + split[1] + "，";
                poetrySuffix = split[2];
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    poetryPrefix = split[0] + "，";
                    poetrySuffix = split[1] + "，" + split[2];
                    //回调函数
                    if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                        logger.warn("从古诗词 API 获取的诗词: \"" + poetry + "\"无法正确分割出两段，将重新调用古诗词 API 获取新的诗词");
                        poetry = PoetryApi.getPoetry(logger).get("content").asText();
                        splitPoetry(poetry);
                    }
                }
            } else {
                logger.warn("从古诗词 API 获取的诗词: \"" + poetry + "\"无法正确分割出两段，将重新调用古诗词 API 获取新的诗词");
                poetry = PoetryApi.getPoetry(logger).get("content").asText();
                splitPoetry(poetry);
            }
        }

        //封装返回数据
        return new String[]{poetryPrefix, poetrySuffix};
    }

}
