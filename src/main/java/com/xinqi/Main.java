package com.xinqi;

import com.xinqi.job.SendMessageJob;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XinQi
 */
public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class);

    public static Map<String, Object> config = new HashMap<>();

    public static void main(String[] args) throws IOException, SchedulerException {
        //得到配置文件的目录
        //获取类当前路径
        String configPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //处理路径
        //去除多余的路径，如：classes
        configPath = configPath.replace(new File(configPath).getName(), "");
        //将字符集转成UTF-8，以去除特殊字符
        configPath = (URLDecoder.decode(configPath, "UTF-8") + "config.yaml");
        //转换成一个完整且规范的路径
        configPath = new File(configPath).getPath();

        //读取配置文件
        logger.info("正在从当前目录下读取配置文件 config.yaml，请检查配置文件是否存在，目录: " + configPath);
        try {
            config = new Yaml().load(Files.newInputStream(Paths.get(configPath)));
        } catch (IOException e) {
            logger.error("无法从" + configPath + "该路径下获取配置文件，请检查该路径是否存在配置文件，配置文件可解压Jar包获取");
            System.exit(0);
        }

        //判断是否有开启推送方式
        if (!(Boolean) config.get("wechat_enable") || !(Boolean) config.get("sms_enable")) {
            logger.info("配置文件未设置任何推送方式，请在配置文件设置 wechat_enable 及 sms_enable，并确保对应推送方式 API 参数内容正确");
            System.exit(0);
        }

        //处理配置文件里的时间
        String cron;
        String time = String.valueOf(config.get("time"));
        //判断是否是时间格式，如果是时间格式则转换为 Cron 表达式
        String timeRegularExpression = "^(?:[01]\\d|2[0-3]):[0-5]\\d$";
        boolean cronMatches = time.matches(timeRegularExpression);
        if (cronMatches) {
            String[] split = time.split(":");
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);
            cron = "0 " + minute + " " + hour + " * * ? *";
            logger.info("已根据配置文件 time: " + time + "，建立的 Cron 表达式: " + cron);
        } else {
            cron = time;
            logger.info("配置文件 time 的格式不为时间格式，将以 Cron 表达式直接使用，表达式为: " + cron + "，请确保表达式正确");
        }

        //开启定时任务
        SchedulerFactory factory = new StdSchedulerFactory();
        // 1.创建调度器 Scheduler
        Scheduler scheduler = factory.getScheduler();
        // 2.创建JobDetail实例，并与MyJob类绑定(Job执行内容)
        JobDetail job = JobBuilder.newJob(SendMessageJob.class).withIdentity("job1", "group1").usingJobData("configPath", configPath).build();
        // 3.构建Trigger实例,根据七子表达式绑定时间
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group1").startNow().withSchedule(CronScheduleBuilder.cronSchedule(cron)).build();
        // 4.执行，开启调度器
        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        //打印日志
        if (cronMatches) {
            logger.info("已成功启动调度器来执行，将在每日" + time + "定时发送天气内容，请确保配置文件各项参数内容正确");
        } else {
            logger.info("已成功启动调度器来执行，将根据七子表达式: " + time + "规则发送天气内容，请确保配置文件各项参数内容正确");
        }
    }
}