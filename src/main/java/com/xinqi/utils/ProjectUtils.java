package com.xinqi.utils;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XinQi
 */
public class ProjectUtils {
    public static Map<String,Object> readConfig(Logger logger, String configPath){
        Map<String, Object> config = new HashMap<>();
        try {
            config = new Yaml().load(Files.newInputStream(Paths.get(configPath)));
        } catch (IOException e) {
            logger.info("无法从" + configPath + "该路径下获取配置文件，请检查该路径是否存在配置文件，配置文件内容参考可在https://github.com/Qing-JiuJiu/Weather-Sms-Push上查看config.yaml文件");
            e.printStackTrace();
            System.exit(0);
        }
        return config;
    }
}
