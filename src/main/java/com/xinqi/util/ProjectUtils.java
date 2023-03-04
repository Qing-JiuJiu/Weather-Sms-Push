package com.xinqi.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author XinQi
 */
public class ProjectUtils {

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

    /**
     * 文本数据gzip解压
     */
    public static String gzipDecompress(byte[] text) throws UnsupportedEncodingException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(text, 0, text.length);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
            byte[] buffer = new byte[256];
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return byteArrayOutputStream.toString("UTF-8");
    }
}
