package com.altratek.altraserver.util;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import com.altratek.altraserver.config.ConfigData;

public final class AltraServerUtils {
	/**
	 * 将文件内容转换为byte[]并返回 支持带签名的utf8和不带签名的utf8格式
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static byte[] readFileToByteArray(String file) throws Exception {
		byte result[] = null;
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int ch = 0;
		while ((ch = bis.read()) != -1) {
			baos.write(ch);
		}
		byte data[] = baos.toByteArray();
		// 如果带签名，则忽略
		if (data[0] == -17 && data[1] == -69 && data[2] == -65) {
			result = new byte[data.length - 3];
			for (int i = 0; i < data.length - 3; i++)
				result[i] = data[i + 3];
		} else
			result = data;
		bis.close();
		baos.close();
		return result;
	}

	public static String getIpBySocketChannel(SocketChannel sc) {
		if (sc == null) {
			return ConfigData.INVALID_IP_ADDRESS;
		}

		Socket st = sc.socket();
		if (st == null) {
			return ConfigData.INVALID_IP_ADDRESS;
		}

		InetAddress addr = st.getInetAddress();
		// null if the socket is not connected
		if (addr == null) {
			return ConfigData.INVALID_IP_ADDRESS;
		}

		return addr.getHostAddress();
	}

	public static String join(Collection<String> s, String delimiter) {
		if (s.isEmpty())
			return "";
		Iterator<String> iter = s.iterator();
		StringBuffer buffer = new StringBuffer(iter.next());
		while (iter.hasNext())
			buffer.append(delimiter).append(iter.next());
		return buffer.toString();
	}

	public static String dateToString(Date date, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(date);
	}

	// java 7以上才有路径拼接方法
	// 为什么不直接+？防止重复分隔符"/"
	public static String combinePath(String path1, String path2) {
		if (path1.length() == 0 || path2.length() == 0) {
			return path1 + path2;
		}
		File file1 = new File(path1);
		File file2 = new File(file1, path2);
		return file2.getPath();
	}
}
