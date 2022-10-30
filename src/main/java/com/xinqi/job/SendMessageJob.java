package com.xinqi.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xinqi.api.SendSmsApi;
import com.xinqi.utils.GzipUtills;
import com.xinqi.utils.HttpsClientUtil;
import com.xinqi.utils.ProjectUtils;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URLEncoder;

import java.util.*;

/**
 * @author XinQi
 */
public class SendMessageJob implements Job {

    Logger logger = LoggerFactory.getLogger(SendMessageJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        logger.info("开始执行每日发送天气信息");

        //得到配置文件路径
        String configPath = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("configPath");

        //读取配置文件，获得相关参数
        Map<String, Object> config = ProjectUtils.readYamlConfig(logger, configPath);
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
        //收件人列表，该注解解除List警告，主要是因为读取来自配置文件，一定会是List<String>
        @SuppressWarnings("unchecked") List<String> addresseeList = (List<String>) config.get("addressee");
        addresseeList.forEach(addressee -> addresseeList.set(addresseeList.indexOf(addressee), "+86" + addressee));
        String[] addresseeArray = addresseeList.toArray(new String[0]);

        //获得诗词
        String[] poetry = getPoetry();

        //下面调用API通用变量
        JsonNode jsonNode;
        String url;
        byte[] response;

        //获得和风天气地区代码和名字
        //先判断配置文件里是否存在天气地区代码，如果存在直接使用，减少Api调用次数
        String regionID = (String) config.get("regionID");
        String regionName = (String) config.get("regionName");
        //如果地区代码不存在，调用API获得地区代码
        if (regionID == null) {
            //获取新的地区代码
            try {
                url = "https://geoapi.qweather.com/v2/city/lookup?key=" + weatherKey + "&location=" + URLEncoder.encode(region, "UTF8");
                logger.info("未在配置文件检测到regionID，正在通过配置文件调用和风天气地区ID获取API获取regionID，获取地区：" + region + "，请求地址：" + url);
                response = HttpsClientUtil.httpsGet(url);
                jsonNode = new ObjectMapper().readTree(GzipUtills.gzipDecompress(response));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            logger.info("和风天气地区ID获取API：" + jsonNode);
            jsonNode = jsonNode.get("location").get(0);
            regionName = jsonNode.get("name").asText();
            regionID = jsonNode.get("id").asText();

            //替换region和regionID
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
            map.put("regionID", regionID);
            yaml.dump(map, buffer);
            try {
                buffer.close();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("通过配置文件region:" + region + "调用和风天气地区ID获取API得到" + regionName + "的regionID:" + regionID + "，已将regionName、regionID写入配置文件，请确保regionName与region地区一致");
        } else {
            logger.info("已从配置文件读取到regionID，将直接使用regionID：" + regionID + "，若需要修改region，请删除已将regionName、regionID整行，否则获取的还是旧地区天气数据");
        }

        //获得当天天气信息
        url = "https://devapi.qweather.com/v7/weather/3d?location=" + regionID + "&key=" + weatherKey;
        logger.info("正在通过和风天气天气获取API获取天气信息，请求地址：" + url);
        try {
            response = HttpsClientUtil.httpsGet(url);
            jsonNode = new ObjectMapper().readTree(GzipUtills.gzipDecompress(response));
        } catch (Exception e) {
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
        String[] parameter = {fxDate, regionName, textDay, humidity, tempMin + "℃ - " + tempMax + "℃", precip, windScaleNight, poetry[0], poetry[1]};
        logger.info("腾讯云传输parameter参数内容：" + Arrays.toString(parameter));
        String smsResponse = SendSmsApi.sendSms(secretId, secretKey, sdkAppId, signName, templateId, addresseeArray, parameter);
        logger.info("腾讯云短信发送API：" + smsResponse);
        logger.info(fxDate + "今日天气已推送，若无收到短信，请检查各项API日志内容。");
    }

    /**
     * 获取随机一首诗词，并切分成前后两段
     */
    public String[] getPoetry() {
        //获得今日好诗的诗词字符串
        String url = "https://v1.jinrishici.com/all";
        logger.info("正在调用古诗词API获取古诗内容，请求地址：" + url);
        byte[] response;
        JsonNode jsonNode;
        try {
            response = HttpsClientUtil.httpsGet(url);
            jsonNode = new ObjectMapper().readTree(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("古诗词API：" + jsonNode);
        String poetry = jsonNode.get("content").asText();
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
                    logger.warn("从古诗词API获取的诗词：\"" + poetry + "\"无法正确分割出两段，将重新调用古诗词API获取新的诗词");
                    getPoetry();
                }
            } else if (split.length == 3) {
                poetryPrefix = split[0] + "，" + split[1] + "，";
                poetrySuffix = split[2];
                if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                    poetryPrefix = split[0] + "，";
                    poetrySuffix = split[1] + "，" + split[2];
                    //回调函数
                    if (poetryPrefix.length() > 12 || poetrySuffix.length() > 12) {
                        logger.warn("从古诗词API获取的诗词：\"" + poetry + "\"无法正确分割出两段，将重新调用古诗词API获取新的诗词");
                        getPoetry();
                    }
                }
            } else {
                logger.warn("从古诗词API获取的诗词：\"" + poetry + "\"无法正确分割出两段，将重新调用古诗词API获取新的诗词");
                getPoetry();
            }
        }

        //封装返回数据
        return new String[]{poetryPrefix, poetrySuffix};
    }
}
