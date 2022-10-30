import com.xinqi.Main;
import com.xinqi.api.WeChatApi;
import com.xinqi.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java.net.URLDecoder;
import java.util.Map;

public class SendWeChat {

    public static void main(String[] args) throws Exception {
        //得到该类日志
        Logger logger = LoggerFactory.getLogger("Test");

        //获取类当前路径
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //处理路径
        path = path.replace(new File(path).getName(), "");
        path = (URLDecoder.decode(path, "UTF-8") + "config.yaml");
        path = new File(path).getPath();
        logger.info("正在从当前目录下读取配置文件config.yaml，请检查配置文件是否存在，目录：" + path);

        //从配置文件读取微信appId
        Map<String, Object> config = ProjectUtils.readYamlConfig(path, logger);

        String app_id = (String)config.get("app_id");
        String app_secret = (String)config.get("app_secret");

        byte[] token = WeChatApi.getToken(app_id, app_secret);
        String s = new String(token);
        System.out.println(s);
    }
}
