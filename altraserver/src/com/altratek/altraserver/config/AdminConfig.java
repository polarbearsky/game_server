package com.altratek.altraserver.config;

import java.util.HashSet;
import java.util.Set;

public class AdminConfig {
	public static Set<String> allowedAdminAddress = new HashSet<String>();
	public static String adminName;
	public static String adminPassword;
	public static boolean isAdminRestricted = true;

	public static boolean validataAdminAddress(String ip) {
		return isAdminRestricted ? allowedAdminAddress.contains(ip) : true;
	}

	public static void addAdminAddress(String ip) {
		// ����Ѿ���*.*.*.*��ip�ˣ����Ժ���ip
		if (!isAdminRestricted) {
			return;
		}

		if (ip.equals("*.*.*.*")) {
			allowedAdminAddress.clear();
			isAdminRestricted = false;
		} else {
			allowedAdminAddress.add(ip);
		}
	}
}
