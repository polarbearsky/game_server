package com.altratek.altraserver;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.config.IpWhiteListConfigReader;
import com.altratek.altraserver.config.IpWhiteListXmlConfigReader;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.AltraServerUtils;

// 同一ip连接数的限制检查。
public class IpFloodChecker {
	public final static IpFloodChecker instance = new IpFloodChecker();

	private ConcurrentHashMap<String, IpCount> ip_count = new ConcurrentHashMap<String, IpCount>();
	private volatile Set<String> noLimitAddress;

	private IpWhiteListConfigReader whiteListReader = null;

	public void init() {
		this.setIpWhiteListReader();
		this.setNoLimitAddress();
	}

	public void reload() {
		this.setNoLimitAddress();
	}

	private void setIpWhiteListReader() {
		if (ConfigData.IP_WHITE_LIST_READER != null) {
			try {
				this.whiteListReader = (IpWhiteListConfigReader) Class.forName(ConfigData.IP_WHITE_LIST_READER)
						.newInstance();
			} catch (Exception e) {
				ServerLogger.error("set ip white list reader error :", e);
				System.exit(1);
			}
		} else {
			this.whiteListReader = new IpWhiteListXmlConfigReader();
		}
	}

	private void setNoLimitAddress() {
		List<String> ipList = this.whiteListReader.read();
		Set<String> ipSet = new HashSet<String>();
		for (String ip : ipList) {
			ipSet.add(ip);
		}
		// reload也是重新赋值
		this.noLimitAddress = ipSet;
	}

	// a mutable value，修改时少查一次hash，用AtomInteger也可以。
	private static class IpCount {
		public short value = 1;
	}

	public synchronized boolean addIp(SocketChannel sc) {
		if (ConfigData.MAX_USERS_PER_IP <= 0) {
			return true;
		}

		String ip = AltraServerUtils.getIpBySocketChannel(sc);
		if (ip.equals(ConfigData.INVALID_IP_ADDRESS)) {
			return false;
		}

		if (isNoLimitAddress(ip)) {
			return true;
		}

		IpCount count = ip_count.get(ip);
		if (count == null) {
			ip_count.put(ip, new IpCount());
			return true;
		}

		if (count.value < ConfigData.MAX_USERS_PER_IP) {
			count.value++;
			return true;
		} else {
			return false;
		}
	}

	synchronized void decreaseIpCount(String ip) {
		if (ConfigData.MAX_USERS_PER_IP <= 0) {
			return;
		}

		IpCount count = ip_count.get(ip);
		if (count == null) {
			return;
		}

		count.value--;
		if (count.value <= 0) {
			ip_count.remove(ip);
		}
	}

	private boolean isNoLimitAddress(String ip) {
		// 先判断ip段，只支持xxx.xxx.xxx格式的ip端。	
		int pos = ip.lastIndexOf(".");
		if (pos > 1) {
			String ipSegment = ip.substring(0, pos);
			if (noLimitAddress.contains(ipSegment)) {
				return true;
			}
		}

		return noLimitAddress.contains(ip);
	}
}
