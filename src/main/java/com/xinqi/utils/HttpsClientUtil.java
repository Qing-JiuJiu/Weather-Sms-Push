package com.xinqi.utils;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * HTTPS请求工具类
 *
 * @author xiaoqqya
 * @version 2021/03/03
 */
public class HttpsClientUtil {

	/**
	 * 封装HTTPS GET请求
	 *
	 * @param urlStr 请求地址
	 * @return 请求结果
	 * @author xiaoqqya
	 */
	public static byte[] httpsGet(String urlStr) throws Exception {

		InputStream input;
		try {
			URL url = new URL(urlStr);

			boolean useHttps = urlStr.startsWith("https");
			if (useHttps) {
				HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
				HostnameVerifier ignoreHostnameVerifier = new MyHostnameVerifier();
				SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
				sslContext.init(null, new TrustManager[]{new MyX509TrustManager()}, new SecureRandom());
				httpsURLConnection.setConnectTimeout(10000);
				httpsURLConnection.setReadTimeout(20000);
				httpsURLConnection.setHostnameVerifier(ignoreHostnameVerifier);
				httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
				httpsURLConnection.connect();
				input = httpsURLConnection.getInputStream();
			} else {
				HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
				httpURLConnection.setConnectTimeout(10000);
				httpURLConnection.setReadTimeout(20000);
				httpURLConnection.connect();
				input = httpURLConnection.getInputStream();
			}
			return toByteArray(input);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	/**
	 * 将输入流转换成字节数组
	 *
	 * @param input 输入流对象
	 * @return 字节数组
	 * @throws IOException IO异常
	 * @author xiaoqqya
	 */
	private static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}
}

/**
 * 实现X509TrustManager接口，信任所有
 *
 * @author xiaoqqya
 * @version 2021/03/03
 */
class MyX509TrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) {

	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) {

	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}

/**
 * 实现HostnameVerifier接口，忽略HTTPS主机验证
 *
 * @author xiaoqqya
 * @version 2021/03/03
 */
class MyHostnameVerifier implements HostnameVerifier {

	@Override
	public boolean verify(String hostname, SSLSession session) {
		return true;
	}
}
