package com.xinqi.utils;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

}
