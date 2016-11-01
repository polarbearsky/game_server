package com.altratek.altraserver.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigData {
	// 总体配置
	public static String SERVER_ADDRESS;
	public static int SERVER_PORT = 9339;
	public static int CROSS_DOMAIN_PORT = 9339;
	// config文件路径
	public static String COMMON_CONFIG_DIR;
	public static String PROCESS_CONFIG_DIR;
	// 日志文件路径
	public static String LOG_DIR;
	// 线程数
	public static boolean ENABLE_SYS_THREAD = true;
	public static int ST_HANDLER_THREADS = 1;
	public static int XT_HANDLER_THREADS = 1;
	public static int OUT_QUEUE_THREADS = 1;
	public static int LOST_HANDLER_THREADS = 2;
	// 事件处理
	public static int MAX_INCOMING_QUEUE = 10000;
	// 消息内容
	public static int MAX_MSG_BYTE_LEN = 4096;
	// 输出
	public static int MAX_CHANNEL_QUEUE = 60;
	public static long MAX_DROPPED_PACKETS = 10L;
	public static int DEAD_CHANNELS_POLICY = 1;
	public static long maxSimultanousConnections;
	// 日志
	public static String LOG4J_XML_FILE = "logConfig.xml";
	// 域安全策略
	public static boolean AUTOSEND_CROSS_DOMAIN = true;
	public static ArrayList<String> ALLOWED_DOMAINS;
	public static boolean EXTERNAL_CROSS_DOAMIN = false;
	// 区域、房间、用户
	public static int SERVER_MAX_CLIENTS = 10000;
	public static int MAX_USERS_PER_IP = 10;
	public static String ADMIN_ZONE_NAME = "$dmn";
	public static int ADMIN_EXT_ID = -1;
	public static int MAX_USER_VARS = -1;
	public static int MAX_ROOM_COUNT = 0x7fffffff;
	public static final int MAX_ROOMS_PER_ZONE = 32768;
	public static int MAX_USER_COUNT = 0x7fffffff;
	public static long MAX_ROOM_VARS = -1;
	// 用户的空闲时间设置（单位：second）
	public static int CONNECTION_CLEANER_INTERVAL = 30;
	public static int MAX_USER_IDLETIME = 3600;
	public static int MAX_GAME_IDLETIME = 300;
	public static int MAX_UNLOGIN_IDLETIME = 120;
	// 扩展
	public static String JAVA_EXTENSIONS_PATH = "javaExtensions/";
	public static String MSG_VALIDATOR = null;
	public static String APP_EVENT_HANDLER = null;
	public static String IP_WHITE_LIST_READER = null;
	public static String USER_BUILDER = null;
	public static String PROCESS_CONFIG_READER = null;
	// 后台任务
	public static long TASK_EXECUTE_INTERVAL = 10L;
	// 无效IP地址
	public static String INVALID_IP_ADDRESS = "----";
	// 开启Logger
	public volatile static boolean ENABLE_PROFILE = false;
	public volatile static boolean ENABLE_PROFILE_EVENT_WAITING_DURATION = false;
	public volatile static boolean ENABLE_MSG_LENGTH_DEBUG = false;

	public static List<String> zoneList;

	// 反馈消息
	public final static String LOGIN_ZONE_FULL = "该区域已经人满，请选择其他区域进入。";
	public final static String LOGIN_ZONE_NOTEXIST = "对不起，该区域不存在！";
	public final static String LOGIN_EMPTY_NAME = "请您使用非空的名字！";
	public final static String LOGIN_SOCKET_NULL = "对不起，您的连接为空！";
	public final static String LOGIN_SOCKET_CLOSED = "对不起，您的连接已经断开";
	public final static String LOGIN_AREADY_IN = "您已经登录了！";
	public final static String LOGIN_NAME_TAKEN = "对不起，该名字已有人使用了！";
	public final static String LOGIN_CLINET_COUNT_FULL = "对不起，服务器端的客户端连接数已满！";

	public final static String JOINROOM_ROOM_NOTEXIST = "对不起，该房间不存在！";
	public final static String JOINROOM_FULL = "对不起，房间人数已满";
	public final static String JOINROOM_AREADY_IN = "您已经在这个房间里了！";

	public final static String CREATEROOM_NAME_AREADY_USED = "对不起，已有同名的房间！";
	public final static String CREATEROOM_COUNT_FULL = "对不起，房间数量过多！";
	public final static String CREATEROOM_CREATECOUNT_FULL = "对不起，您已经达到最大房间创建数，不能再创建！";
	public final static String CREATEROOM_ROOM_COUNT_FULL = "对不起，本区域的房间数已满";

	// 把进程相关的配置合并到这里，便于读取
	public static void mergeProcessConfigData(ProcessConfigData pData) {
		SERVER_ADDRESS = pData.ip;
		SERVER_PORT = pData.port;
		CROSS_DOMAIN_PORT = SERVER_PORT;
		zoneList = pData.zoneNames;

		// 以下这些参数应用配置可以有，进程配置也可以用，后者覆盖前者
		// 这个机制的目的是简化配置（每个进程都配置麻烦），
		// 同时保留进程可以单独定义的灵活性，用于调优
		if (pData.extHandlerThreads != null)
			XT_HANDLER_THREADS = pData.extHandlerThreads;

		if (pData.lostHandlerThread != null)
			LOST_HANDLER_THREADS = pData.lostHandlerThread;

		if (pData.outQueueThreads != null)
			OUT_QUEUE_THREADS = pData.outQueueThreads;

		if (pData.sysHandlerThreads != null)
			ST_HANDLER_THREADS = pData.sysHandlerThreads;

		if (pData.enableMsgLengthDebug != null)
			ENABLE_MSG_LENGTH_DEBUG = pData.enableMsgLengthDebug;

		if (pData.enableProfile != null)
			ENABLE_PROFILE = pData.enableProfile;
	}
}