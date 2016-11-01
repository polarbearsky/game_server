package com.altratek.altraserver.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigData {
	// ��������
	public static String SERVER_ADDRESS;
	public static int SERVER_PORT = 9339;
	public static int CROSS_DOMAIN_PORT = 9339;
	// config�ļ�·��
	public static String COMMON_CONFIG_DIR;
	public static String PROCESS_CONFIG_DIR;
	// ��־�ļ�·��
	public static String LOG_DIR;
	// �߳���
	public static boolean ENABLE_SYS_THREAD = true;
	public static int ST_HANDLER_THREADS = 1;
	public static int XT_HANDLER_THREADS = 1;
	public static int OUT_QUEUE_THREADS = 1;
	public static int LOST_HANDLER_THREADS = 2;
	// �¼�����
	public static int MAX_INCOMING_QUEUE = 10000;
	// ��Ϣ����
	public static int MAX_MSG_BYTE_LEN = 4096;
	// ���
	public static int MAX_CHANNEL_QUEUE = 60;
	public static long MAX_DROPPED_PACKETS = 10L;
	public static int DEAD_CHANNELS_POLICY = 1;
	public static long maxSimultanousConnections;
	// ��־
	public static String LOG4J_XML_FILE = "logConfig.xml";
	// ��ȫ����
	public static boolean AUTOSEND_CROSS_DOMAIN = true;
	public static ArrayList<String> ALLOWED_DOMAINS;
	public static boolean EXTERNAL_CROSS_DOAMIN = false;
	// ���򡢷��䡢�û�
	public static int SERVER_MAX_CLIENTS = 10000;
	public static int MAX_USERS_PER_IP = 10;
	public static String ADMIN_ZONE_NAME = "$dmn";
	public static int ADMIN_EXT_ID = -1;
	public static int MAX_USER_VARS = -1;
	public static int MAX_ROOM_COUNT = 0x7fffffff;
	public static final int MAX_ROOMS_PER_ZONE = 32768;
	public static int MAX_USER_COUNT = 0x7fffffff;
	public static long MAX_ROOM_VARS = -1;
	// �û��Ŀ���ʱ�����ã���λ��second��
	public static int CONNECTION_CLEANER_INTERVAL = 30;
	public static int MAX_USER_IDLETIME = 3600;
	public static int MAX_GAME_IDLETIME = 300;
	public static int MAX_UNLOGIN_IDLETIME = 120;
	// ��չ
	public static String JAVA_EXTENSIONS_PATH = "javaExtensions/";
	public static String MSG_VALIDATOR = null;
	public static String APP_EVENT_HANDLER = null;
	public static String IP_WHITE_LIST_READER = null;
	public static String USER_BUILDER = null;
	public static String PROCESS_CONFIG_READER = null;
	// ��̨����
	public static long TASK_EXECUTE_INTERVAL = 10L;
	// ��ЧIP��ַ
	public static String INVALID_IP_ADDRESS = "----";
	// ����Logger
	public volatile static boolean ENABLE_PROFILE = false;
	public volatile static boolean ENABLE_PROFILE_EVENT_WAITING_DURATION = false;
	public volatile static boolean ENABLE_MSG_LENGTH_DEBUG = false;

	public static List<String> zoneList;

	// ������Ϣ
	public final static String LOGIN_ZONE_FULL = "�������Ѿ���������ѡ������������롣";
	public final static String LOGIN_ZONE_NOTEXIST = "�Բ��𣬸����򲻴��ڣ�";
	public final static String LOGIN_EMPTY_NAME = "����ʹ�÷ǿյ����֣�";
	public final static String LOGIN_SOCKET_NULL = "�Բ�����������Ϊ�գ�";
	public final static String LOGIN_SOCKET_CLOSED = "�Բ������������Ѿ��Ͽ�";
	public final static String LOGIN_AREADY_IN = "���Ѿ���¼�ˣ�";
	public final static String LOGIN_NAME_TAKEN = "�Բ��𣬸�����������ʹ���ˣ�";
	public final static String LOGIN_CLINET_COUNT_FULL = "�Բ��𣬷������˵Ŀͻ���������������";

	public final static String JOINROOM_ROOM_NOTEXIST = "�Բ��𣬸÷��䲻���ڣ�";
	public final static String JOINROOM_FULL = "�Բ��𣬷�����������";
	public final static String JOINROOM_AREADY_IN = "���Ѿ�������������ˣ�";

	public final static String CREATEROOM_NAME_AREADY_USED = "�Բ�������ͬ���ķ��䣡";
	public final static String CREATEROOM_COUNT_FULL = "�Բ��𣬷����������࣡";
	public final static String CREATEROOM_CREATECOUNT_FULL = "�Բ������Ѿ��ﵽ��󷿼䴴�����������ٴ�����";
	public final static String CREATEROOM_ROOM_COUNT_FULL = "�Բ��𣬱�����ķ���������";

	// �ѽ�����ص����úϲ���������ڶ�ȡ
	public static void mergeProcessConfigData(ProcessConfigData pData) {
		SERVER_ADDRESS = pData.ip;
		SERVER_PORT = pData.port;
		CROSS_DOMAIN_PORT = SERVER_PORT;
		zoneList = pData.zoneNames;

		// ������Щ����Ӧ�����ÿ����У���������Ҳ�����ã����߸���ǰ��
		// ������Ƶ�Ŀ���Ǽ����ã�ÿ�����̶������鷳����
		// ͬʱ�������̿��Ե������������ԣ����ڵ���
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