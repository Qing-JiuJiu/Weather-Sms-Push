package com.xinqi.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author XinQi
 */
public class GzipUtills {

    /**
     * 文本数据gzip压缩
     */
    public static String gzipCompress(String text) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
            gzipOutputStream.flush();
            gzipOutputStream.finish();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    /**
     * 文本数据gzip解压
     */
    public static String gzipDecompress(byte[] text) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //如果需要解码使用
        //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(text));
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

        return byteArrayOutputStream.toString();
    }

}
