package com.altratek.altraserver.config;

import java.util.List;

public class IpWhiteListXmlConfigReader implements IpWhiteListConfigReader {

	@Override
	public List<String> read() {	
		return ConfigReader.loadIpWhiteList();
	}
}
