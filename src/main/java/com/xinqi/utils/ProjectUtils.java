package com.xinqi.utils;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author XinQi
 */
public class ProjectUtils {

    /**
     * 读取配置文件
     */
    public static Map<String, Object> readYamlConfig(String configPath, Logger logger) {
        Map<String, Object> config = new HashMap<>(16);
        try {
            config = new Yaml().load(Files.newInputStream(Paths.get(configPath)));
        } catch (IOException e) {
            logger.error("无法从" + configPath + "该路径下获取配置文件，请检查该路径是否存在配置文件，配置文件内容参考可在https://github.com/Qing-JiuJiu/Weather-Sms-Push上查看config.yaml文件");
            e.printStackTrace();
            System.exit(0);
        }
        return config;
    }

    /**
     * 获取随机颜色
     */
    public static String getColor(){
        //红色
        String red;
        //绿色
        String green;
        //蓝色
        String blue;
        //生成随机对象
        Random random = new Random();
        //生成红色颜色代码
        red = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成绿色颜色代码
        green = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成蓝色颜色代码
        blue = Integer.toHexString(random.nextInt(256)).toUpperCase();

        //判断红色代码的位数
        red = red.length()==1 ? "0" + red : red ;
        //判断绿色代码的位数
        green = green.length()==1 ? "0" + green : green ;
        //判断蓝色代码的位数
        blue = blue.length()==1 ? "0" + blue : blue ;
        //生成十六进制颜色值
        return  "#"+red+green+blue;
    }

    /**
     * 根据日期获得星期
     * @return 当前日期是星期几
     */
    public static String getDateWeekTime(String sDate){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
            Date date=sdf.parse(sDate);
            SimpleDateFormat format = new SimpleDateFormat("EEEE", Locale.SIMPLIFIED_CHINESE);
            return format.format(date);
        }catch(Exception ex){
            System.out.println("TimeUtil getFullDateWeekTime Error:"+ex.getMessage());
            return "";
        }
    }
}
