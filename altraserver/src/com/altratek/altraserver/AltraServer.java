package com.altratek.altraserver;

import java.io.IOException;
import java.net.BindException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.config.ConfigReader;
import com.altratek.altraserver.config.DefaultProcessConfigReader;
import com.altratek.altraserver.config.ProcessConfigData;
import com.altratek.altraserver.config.ProcessConfigReader;
import com.altratek.altraserver.config.reloader.ConfigReloaderManager;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.domain.userbuilder.DefaultUserBuilder;
import com.altratek.altraserver.domain.userbuilder.IUserBuilder;
import com.altratek.altraserver.handler.ExtensionHandler;
import com.altratek.altraserver.handler.LostHandler;
import com.altratek.altraserver.handler.SystemHandler;
import com.altratek.altraserver.lib.validator.DefaultMessageValidator;
import com.altratek.altraserver.lib.validator.IMessageValidator;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.logger.SimpleLogger;
import com.altratek.altraserver.task.IdleConnCheckTask;
import com.altratek.altraserver.util.AltraServerUtils;
import com.altratek.altraserver.util.ServerStat;

public class AltraServer extends Thread {
	private static AltraServer instance;
	private static CmdArgs cmdArgs;

	public volatile boolean shutdowning = false;
	private long serverStartTime;

	private final ChannelAcceptor channelAcceptor = new ChannelAcceptor();
	private final ChannelSelector channelSelector = new ChannelSelector();

	private final ZoneManager zoneManager = ZoneManager.instance;
	private final ChannelManager channelManager = ChannelManager.instance;

	private ServerWriter serverWriter;
	private SystemHandler systemHandler;
	private ExtensionHandler extensionHandler;
	private LostHandler lostHandler;

	private ConfigReader configReader;

	private ScheduledExecutorService scheduler;

	private ConfigReloaderManager configReloaderManager;
	private ApplicationEventHandler appEventHandler;

	private IMessageValidator messageValidator;
	private IUserBuilder userBuilder;
	private ProcessConfigReader processConfigReader;

	private AltraServer() {
		super("main");

		configReader = ConfigReader.getInstance();

		configReloaderManager = new ConfigReloaderManager();
	}

	public static void main(String args[]) {
		try {
			cmdArgs = new CmdArgs(args);
		} catch (Exception e) {
			System.exit(1);
		}

		ConfigData.COMMON_CONFIG_DIR = cmdArgs.commonConfigDir;
		ConfigData.PROCESS_CONFIG_DIR = cmdArgs.processConfigDir;
		ConfigData.LOG_DIR = cmdArgs.logDir;

		SimpleLogger.init(ConfigData.LOG_DIR);

		SimpleLogger.info(cmdArgs);

		instance = new AltraServer();
		instance.start();
	}

	public void run() {
		welcomeMsg();
		initLogger();
		loadServerConfig();
		setAppEventHandler();

		// 给扩展一个时机做些早期的必要的初始化工作。
		// 例如初始化配置所在的数据库
		this.appEventHandler.onStart();

		// 读取进程相关的配置，可能是本地文件读，也可能是远程中心读取
		loadProcessConfigAndSetLogLevel();

		this.logProfileSwitch();

		this.zoneManager.init();
		this.zoneManager.createAdminZoneAndRoom();

		this.lostHandler = new LostHandler();
		this.lostHandler.init();

		serverWriter = new ServerWriter(ConfigData.OUT_QUEUE_THREADS);
		systemHandler = new SystemHandler();
		extensionHandler = new ExtensionHandler();
		extensionHandler.init();

		setMessageValidator();

		setUserBuilder();

		this.appEventHandler.beforeExtensionInit();

		loadZoneConfig();

		this.appEventHandler.afterExtensionInit();

		initServerSocket();

		InMsgHandleFacade.init();

		IpFloodChecker.instance.init();

		serverStartTime = System.currentTimeMillis();

		this.channelSelector.start();

		ServerLogger.info("init scheduled tasks ...");

		scheduler = Executors.newSingleThreadScheduledExecutor(new PoolThreadFactory("ServerScheduler"));
		scheduler.scheduleAtFixedRate(new IdleConnCheckTask(), 30, ConfigData.CONNECTION_CLEANER_INTERVAL,
				TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(ServerStat.instance, 120, ServerStat.STAT_INTERVAL, TimeUnit.SECONDS);

		ServerLogger.info("init scheduled tasks ok.\n");

		SimpleLogger.info("threads :");

		logThreadPool("Selector", 1);
		logThreadPool("Writer", ConfigData.OUT_QUEUE_THREADS);
		if (InMsgHandleFacade.enableSystemThread) {
			logThreadPool("SystemHandler", InMsgHandleFacade.systemHandlerThreadCount());
			logThreadPool("ExtensionHandler", InMsgHandleFacade.extensionHandlerThreadCount());
		} else {
			logThreadPool("MessageHandler", InMsgHandleFacade.extensionHandlerThreadCount());
		}
		logThreadPool("LostHandler", this.lostHandler.getThreadCount());

		ServerLogger.info("");

		ServerLogger.info("AltraServer is now running!");

		// 由于刷屏太快，welcome message中版本号不方便看见，所以在这里再显示一次。
		ServerLogger.error("The version is : " + Version.version);

		this.channelAcceptor.start();
	}

	private void logThreadPool(String threadName, int threadCount) {
		SimpleLogger.infof("%s : %s thread(s)", threadName, threadCount);
	}

	private void welcomeMsg() {
		SimpleLogger.info("");
		SimpleLogger.info("Baitian AltraServer start...");
		SimpleLogger.info("Version : " + Version.version);
		SimpleLogger.info("");
	}

	private void loadServerConfig() {
		SimpleLogger.info("load server config ...");
		try {
			configReader.readServerConfig();

			SimpleLogger.info("load server config ok.\n");
		} catch (Exception ce) {
			ErrorInConfig("load config", ce);
		}
	}

	private void loadProcessConfigAndSetLogLevel() {
		this.processConfigReader = this.<ProcessConfigReader, DefaultProcessConfigReader> newInstanceByConfig(
				ConfigData.PROCESS_CONFIG_READER, DefaultProcessConfigReader.class);
		try {
			this.processConfigReader.setProperty(cmdArgs.processId, cmdArgs.commonConfigDir, cmdArgs.processConfigDir);
			ProcessConfigData pData = this.processConfigReader.readConfigData();
			ConfigData.mergeProcessConfigData(pData);
			ServerLogger.SetLevel(pData.logLevel);
		} catch (Exception ex) {
			ErrorInConfig("load process config", ex);
		}
	}

	private void loadZoneConfig() {
		SimpleLogger.info("load zone ...");
		try {
			ZoneManager.instance.loadZones(configReader.getZoneDetailConfigNode());
		} catch (Throwable e) {
			ErrorInConfig("load zone", e);
		}
		SimpleLogger.info("load zone ok.\n");
	}

	private void logProfileSwitch() {
		SimpleLogger.infof("enable profile : %s", ConfigData.ENABLE_PROFILE);
		SimpleLogger.infof("enable msg len profile : %s", ConfigData.ENABLE_MSG_LENGTH_DEBUG);
		SimpleLogger.infof("main log level : %s", ServerLogger.getLevel());
	}

	private void ErrorInConfig(String msg, Throwable error) {
		SimpleLogger.error("start server error - " + msg + " : ", error);
		System.exit(1);
	}

	private void initServerSocket() {
		try {
			this.channelManager.init();
			this.channelAcceptor.init();
			this.channelSelector.init(this.channelAcceptor);

			SimpleLogger.infof("Server socket info : %s / %s\n", ConfigData.SERVER_ADDRESS, ConfigData.SERVER_PORT);
		} catch (BindException be) {
			ErrorInConfig("<<Init ServerSocket>>: The Port[" + ConfigData.SERVER_PORT + "] is being used - ", be);
		} catch (IOException ioe) {
			ErrorInConfig("<<Init ServerSocket>>: IOException - ", ioe);
		}
	}

	private void initLogger() {
		SimpleLogger.info("init logger ...");

		String logConfigFileDir;
		boolean appendLogDir;
		// 如果进程配置中心化，就没有进程配置文件目录了，所以配置都在一个配置文件目录。
		if (cmdArgs.readProcessConfigLocal()) {
			logConfigFileDir = cmdArgs.processConfigDir;
			appendLogDir = false;
		} else {
			logConfigFileDir = cmdArgs.commonConfigDir;
			appendLogDir = true;
		}

		try {
			ServerLogger.initLogger(logConfigFileDir, appendLogDir);
		} catch (Exception e) {
			SimpleLogger.error("init logger error : ", e);
			System.exit(1);
		}

		SimpleLogger.info("init logger ok.");
	}

	public void lostInvalidMsgConn(User user, SocketChannel sc, String reason) {
		// user可能是null。
		ServerLogger.invalidMsg(user, reason);
		this.channelManager.lostConn(sc, reason);
	}

	public void lostConn(SocketChannel sc, String userLostParam) {
		this.channelManager.lostConn(sc, userLostParam);
	}

	public SelectionKey getChannelKey(SocketChannel sc) {
		return this.channelSelector.getChannelKey(sc);
	}

	public static AltraServer getInstance() {
		return instance;
	}

	public ServerWriter getServerWriter() {
		return serverWriter;
	}

	public SystemHandler getSystemHandler() {
		return systemHandler;
	}

	public ExtensionHandler getExtensionHandler() {
		return extensionHandler;
	}

	LostHandler getLostHandler() {
		return this.lostHandler;
	}

	public long getServerStartTime() {
		return serverStartTime;
	}

	public IMessageValidator getMessageValidator() {
		return messageValidator;
	}

	public IUserBuilder getUserBuilder() {
		return userBuilder;
	}

	public ConfigReloaderManager getConfigReloaderManager() {
		return configReloaderManager;
	}

	private void setAppEventHandler() {
		this.appEventHandler = this.<ApplicationEventHandler, ApplicationEventHandler> newInstanceByConfig(
				ConfigData.APP_EVENT_HANDLER, ApplicationEventHandler.class);
	}

	private void setMessageValidator() {
		this.messageValidator = this.<IMessageValidator, DefaultMessageValidator> newInstanceByConfig(
				ConfigData.MSG_VALIDATOR, DefaultMessageValidator.class);
	}

	private void setUserBuilder() {
		this.userBuilder = this.<IUserBuilder, DefaultUserBuilder> newInstanceByConfig(ConfigData.USER_BUILDER,
				DefaultUserBuilder.class);
	}

	@SuppressWarnings("unchecked")
	private <B, D extends B> B newInstanceByConfig(String className, Class<D> defaultType) {
		B instance;
		try {
			if (className == null || className.length() == 0) {
				instance = defaultType.newInstance();
			} else {
				instance = (B) Class.forName(className).newInstance();
			}
			return instance;
		} catch (Exception e) {
			ErrorInConfig("set " + className, e);
			return null; // just for avoid compile error, system.exit in ErrorInConfig
		}
	}

	// 如果这个匿名类到关服时才加载，会有这样一个问题：
	// 在程序启动之后，如果更换了新jar包，在内存中运行的程序AltraServer类是旧jar包的，
	// 收到关服务命令时，因为之前未加载，要从新jar包加载这个Runnable匿名类，
	// 类加载机制会导致这种加载失败，从而关服失败。
	// 为解决这个问题，提前把这个类给加载了，所以定义成成员变量，并初始化。
	private Runnable shutDownTask = new Runnable() {
		@Override
		public void run() {
			shutdown();
			System.exit(0);
		}
	};

	public void poweroff() {
		if (shutdowning) {
			return;
		}

		shutdowning = true;
		ServerLogger.info("\n\nServer is shutting down...\n\n");

		Thread t = new Thread(shutDownTask, "shutdown");
		t.setDaemon(false); // it is probably the last thread, CAN NOT BE DAEMON !
		t.start();
	}

	private void shutdown() {
		try {
			String now = AltraServerUtils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss");
			shutdownLog(String.format("server(%s) start shutdown at : %s ...", Version.version, now));

			startShutdownLog("scheduler");
			scheduler.shutdown();
			scheduler.awaitTermination(10, TimeUnit.SECONDS);
			endShutdownLog("scheduler");

			startShutdownLog("acceptor");
			this.channelAcceptor.shutdown();
			endShutdownLog("acceptor");

			startShutdownLog("selector");
			this.channelSelector.shutdown();
			endShutdownLog("selector");

			startShutdownLog("writer");
			serverWriter.shutdown();
			endShutdownLog("writer");

			startShutdownLog("inMsgHandlers");
			InMsgHandleFacade.shutdown();
			endShutdownLog("inMsgHandlers");

			// 会触发user lost
			shutdownLog("start lost all users...");
			this.channelManager.closeAllChannels();
			shutdownLog("end lost all users.");

			startShutdownLog("lostService");
			this.lostHandler.shutdown();
			endShutdownLog("lostService");

			assertZoneEmpty();

			shutdownLog("start destory extensions...");
			this.appEventHandler.beforeExtensionDestory();
			this.zoneManager.destroyExtensions();
			this.appEventHandler.afterExtensionDestory();
			shutdownLog("end destory extension.");

			closeSocket();
			shutdownLog("socket closed.");

			ServerLogger.destroy();

			shutdownLog("server has shutdown.\n\n");
		} catch (Throwable e) {
			e.printStackTrace();
			ServerLogger.error("shutting down error : ", e);
		}
	}

	private static void startShutdownLog(String item) {
		SimpleLogger.info(item + " start shutdown...");
	}

	private static void endShutdownLog(String item) {
		SimpleLogger.info(item + " has shutdown.");
	}

	private static void shutdownLog(String msg) {
		SimpleLogger.info(msg);
	}

	private void assertZoneEmpty() {
		int total = 0;
		List<Zone> zones = this.zoneManager.getAllZones();
		for (Zone z : zones) {
			total += z.getUserCount();
		}

		if (total != 0) {
			ServerLogger.error("WHY THERE ARE USERS IN THE SERVER AT THIS MOMENT !!!");
		}
	}

	private void closeSocket() throws IOException {
		this.channelAcceptor.closeServerSocket();
	}

	public String getIpBySocketChannel(SocketChannel sc) {
		return AltraServerUtils.getIpBySocketChannel(sc);
	}

	public void stopServerForTest() {
		if (!shutdowning) {
			System.out.println("\nServer is shutting down...\n\n");
			shutdown();
		}
	}
}