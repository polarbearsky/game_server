package com.altratek.altraserver.config;

public abstract class ProcessConfigReader {
	protected String processId;
	protected String commonConfigFileDir;
	protected String processConfigFileDir;

	public abstract ProcessConfigData readConfigData() throws Exception;

	public final void setProperty(String processId, String commonConfigFileDir, String processConfigFileDir) {
		this.processId = processId;
		this.commonConfigFileDir = commonConfigFileDir;
		this.processConfigFileDir = processConfigFileDir;
	}
}
