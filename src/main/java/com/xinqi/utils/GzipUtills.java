package com.xinqi.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

/**
 * @author XinQi
 */
public class GzipUtills {

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
