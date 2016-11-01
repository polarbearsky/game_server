package com.netty.game.server.config;

public class ServerConfigData {
	public static final int DEFAULT_SERVER_PORT = 8989;
	
	/************pipeline相关******************/
	public static final String LOGIN_HANDLER_NAME = "loginHandler";
	public static final String EXT_HANDLER_NAME = "extHandler";
	
	/************cmd相关*************************/
	public static final int CMD_LOGIN = 1;
	public static final int CMD_EXT = 2;
	
	
	
	/***************key相关*************************/
	public static final String KEY_PARAM_USER_NAME = "username";
	public static final String KEY_PARAM_PASSWORD = "pwd";
	
	public static final String KEY_SUB_CMD = "subCmd";
	public static final String KEY_SUBCMD_PARAM = "subCmdParam";
}
