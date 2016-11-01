package com.altratek.altraserver.extensions;

public class ExtensionItem {

	private String className;
	private String zoneName;

	public ExtensionItem(String className, String zoneName) {
		this.className = className;
		this.zoneName = zoneName;
	}

	public String getClassName() {
		return className;
	}

	public String getZoneName() {
		return zoneName;
	}
}
