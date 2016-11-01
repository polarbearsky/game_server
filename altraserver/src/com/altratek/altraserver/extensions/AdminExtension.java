package com.altratek.altraserver.extensions;

import java.nio.channels.SocketChannel;
import java.util.HashMap;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ChannelManager;
import com.altratek.altraserver.ZoneManager;
import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.config.AdminConfig;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.config.reloader.ConfigReloaderManager;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.event.Listen;
import com.altratek.altraserver.event.LoginEvent;
import com.altratek.altraserver.event.SystemEvent;
import com.altratek.altraserver.exception.LoginException;
import com.altratek.altraserver.lib.ActionscriptObject;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgSender;

public class AdminExtension extends AbstractExtension {	
	private AltraServer als = AltraServer.getInstance();
	private static final int ONE_DAY = 0x5265c00;
	private static final int ONE_HOUR = 0x36ee80;
	private static final int ONE_MINUTE = 60000;
	private HashMap<String, Cmd> cmds;

	private enum Cmd {
		Halt, MsgLenDebugSwitch, ProfSwitch, Reloader, ReloadConfig, ReloadExConfig, Status, Msg
	}

	public AdminExtension() {		
		initCmds();
	}

	private void initCmds() {
		cmds = new HashMap<String, Cmd>();
		cmds.put("halt", Cmd.Halt);
		cmds.put("msgLenDebugSwitch", Cmd.MsgLenDebugSwitch);
		cmds.put("profSwitch", Cmd.ProfSwitch);
		cmds.put("reloader", Cmd.Reloader);
		cmds.put("reloadConfig", Cmd.ReloadConfig);
		cmds.put("reloadExConfig", Cmd.ReloadExConfig);
		cmds.put("status", Cmd.Status);
		cmds.put("msg", Cmd.Msg);
	}

	@Listen( { SystemEvent.Login })
	public void handleInternalEvent(SystemEvent event) {
		switch (event.eventType) {
		case SystemEvent.Login:
			handleLogin(event);
			break;
		default:
			refuse(event.toString());
			break;
		}
	}

	// 目前的管理员登录没有加密保护
	private void handleLogin(SystemEvent event) {
		String eventZoneName = event.zone.getName();
		LoginEvent loginEvent = (LoginEvent) event;
		if (eventZoneName.equals(ConfigData.ADMIN_ZONE_NAME)) {
			String nick = loginEvent.userName;
			String pwd = loginEvent.password;
			SocketChannel channel = loginEvent.socketChannel;
			String ip = channel.socket().getInetAddress().toString().substring(1);
			try {
				if (AdminConfig.validataAdminAddress(ip)) {
					if (nick.equals(AdminConfig.adminName) && pwd.equals(AdminConfig.adminPassword)) {
						User user = ChannelManager.instance.doLogin(nick, pwd, event.zone, channel);
						user.setAdmin();
						ActionscriptObject response = new ActionscriptObject();
						response.putString("_cmd", "logOK");
						response.putString("name", user.getName());
						response.putNumber("id", user.getUserId());
						ExtensionHelper.instance().sendXtResponse(response, channel);
					} else {
						throw new LoginException("Invalid name or password, try again!");
					}
				} else {
					throw new LoginException("Your IP address is not allowed to connect as admin.");
				}
			} catch (LoginException le) {
				ActionscriptObject response = new ActionscriptObject();
				response.putString("_cmd", "logKO");
				response.putString("err", le.getLocalizedMessage());
				ExtensionHelper.instance().sendXtResponse(response, channel);
			}
		}
	}

	public void handleRequest(String cmd, String as[], User user, int fromRoom) {
	}

	public void handleRequest(byte cmd, IoBuffer byteBuffer, User user, int fromRoom) {
	}

	public void handleRequest(String cmd, ActionscriptObject asObj, User user, int fromRoom) {
		Cmd currentCmd = cmds.get(cmd);
		if (currentCmd == null)
			return;
		if (user.isAdmin()) {
			switch (currentCmd) {
			case Halt:
				sendHaltSignal(asObj, user);
				break;
			case MsgLenDebugSwitch:
				msgLenDebugSwitch(asObj);
				break;
			case ProfSwitch:
				profSwitch(asObj);
				break;
			case Reloader:
				reloader(asObj);
				break;
			case ReloadConfig:
				reloadConfig();
				break;
			case ReloadExConfig:
				reloadExConfig();
				break;
			case Status:
				sendServerStatus(user);
				break;
			case Msg:
				sendAdminMsg(asObj);
				break;
			default:
				refuse(currentCmd.toString(), user);
				break;
			}
		} else {
			ServerLogger.error(new StringBuilder(
					"<<HandleAdminRequest>>: A non-Admin user has sent an Admin request - Req[").append(cmd).append(
					"] - User[").append(user.getName()).append("] - IP[").append(user.getIP()).append("]").toString());
		}
	}

	private void sendHaltSignal(ActionscriptObject ao, User user) {		
		als.poweroff();
	}

	private void sendServerStatus(User user) {
		ActionscriptObject response = new ActionscriptObject();
		Runtime rt = Runtime.getRuntime();

		response.putString("_cmd", "status");
		response.putString("upTime", getUptime());
		//response.putString("dataIn", String.valueOf(ConfigData.dataIN));
		//response.putString("dataOut", String.valueOf(ConfigData.dataOUT));

		int load = ChannelManager.instance.getGlobalUserCount() * ConfigData.MAX_CHANNEL_QUEUE;
		int outGoingMessagesQSize = 0;
		load = (outGoingMessagesQSize * 100) / load;
		response.putString("load", String.valueOf(load));

		response.putString("vmMax", String.valueOf(rt.maxMemory()));
		response.putString("vmUsed", String.valueOf(rt.maxMemory() - rt.freeMemory()));

		response.putString("omq", String.valueOf(outGoingMessagesQSize));
		//response.putString("shq", String.valueOf(sh.getEventQueueSize()));
		//response.putString("ehq", String.valueOf(als.getExtensionHandler().getEventQueueSize()));

		response.putString("s_zones", String.valueOf(ZoneManager.instance.getZoneCount()));
		response.putString("s_rooms", String.valueOf(ZoneManager.instance.getRoomCount()));
		response.putString("s_users", String.valueOf(ChannelManager.instance.getGlobalUserCount()));
		response.putString("s_max", String.valueOf(ConfigData.maxSimultanousConnections));
		//response.putString("s_dropIn", String.valueOf(ConfigData.inComingDroppedMessages));
		//response.putString("s_dropOut", String.valueOf(ConfigData.outGoingDroppedMessages));
		response.putString("s_sockets", String.valueOf(ChannelManager.instance.getChannelCount()));
		response.putString("s_threads", String.valueOf(Thread.activeCount()));

		this.sendResponse(response, user);
	}

	/**
	 * 以天数:小时数:分钟数 的格式 返回自上次启动以来的时间
	 * 
	 * @return
	 */
	private String getUptime() {
		StringBuilder result = new StringBuilder();
		long now = System.currentTimeMillis();
		long start = als.getServerStartTime();
		long temp = 0L;
		long elapsed = now - start;
		int days = (int) Math.floor(((double) elapsed) / ONE_DAY);
		temp = ONE_DAY * (long) days;
		elapsed -= temp;
		int hours = (int) Math.floor(((double) elapsed) / ONE_HOUR);
		temp = ((long) ONE_HOUR) * hours;
		elapsed -= temp;
		int minutes = (int) Math.floor(((double) elapsed) / ONE_MINUTE);
		String s_days = String.valueOf(days);
		for (int i = 0; i < 4 - s_days.length(); i++)
			result.append("0");
		result.append(s_days);
		result.append(":");
		if (hours < 10)
			result.append("0");
		result.append(hours);
		result.append(":");
		if (minutes < 10)
			result.append("0");
		result.append(minutes);
		return result.toString();
	}

	private void refuse(String cmdName) {
		StringBuilder sb = new StringBuilder("<<AdminExtension.handleInternalEvent()>>: Refused req[").append(cmdName)
				.append("]");
		ServerLogger.error(sb.toString());
	}

	private void refuse(String cmdName, User user) {
		StringBuilder sb = new StringBuilder("<<AdminExtension.handleRequest()>>: Refused req[").append(cmdName)
				.append("] - User[").append(user.getName()).append("] - IP[").append(user.getIP()).append("]");
		ServerLogger.error(sb.toString());
	}

	private void msgLenDebugSwitch(ActionscriptObject ao) {
		boolean enable = ao.getBool("enable");
		ConfigData.ENABLE_MSG_LENGTH_DEBUG = enable;
	}

	private void profSwitch(ActionscriptObject ao) {
		boolean enable = ao.getBool("enable");
		ConfigData.ENABLE_PROFILE = enable;
	}

	private void reloader(ActionscriptObject ao) {
		String reloaderName = ao.getString("name");
		String param = ao.getString("param");

		ServerLogger.info(new StringBuilder("Reloader[").append(reloaderName).append("] starts").toString());
		als.getConfigReloaderManager().reloadConfig(reloaderName, param);
	}

	private void reloadConfig() {
		als.getConfigReloaderManager().reloadConfig(ConfigReloaderManager.TYPE_CONFIG_RELOADER, "");
	}

	private void reloadExConfig() {
		als.getConfigReloaderManager().reloadConfig(ConfigReloaderManager.TYPE_EXCONFIG_RELOADER, "");
	}

	private void sendAdminMsg(ActionscriptObject ao) {
		String content = ao.getString("content");
		MsgSender.sendAdminMessageToAll(content);
	}
}