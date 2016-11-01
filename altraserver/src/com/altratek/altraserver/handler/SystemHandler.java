package com.altratek.altraserver.handler;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ServerWriter;
import com.altratek.altraserver.ZoneManager;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.event.SystemEventDispatcher;
import com.altratek.altraserver.exception.JoinRoomException;
import com.altratek.altraserver.lib.IServerEventHandler;
import com.altratek.altraserver.lib.RequestEvent;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgSender;
import com.altratek.altraserver.message.ReqMsgType;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.request.AsObjMessage;
import com.altratek.altraserver.message.request.JoinRoomMessage;
import com.altratek.altraserver.message.request.LeaveRoomMessage;
import com.altratek.altraserver.message.request.LoginMessage;
import com.altratek.altraserver.message.request.PubMsgMessage;
import com.altratek.altraserver.message.request.RequestMessage;
import com.altratek.altraserver.message.request.SetRvarsMessage;
import com.altratek.altraserver.message.request.SetUvarsMessage;
import com.altratek.altraserver.message.response.DataObjMessage;
import com.altratek.altraserver.message.response.LogKOMessage;
import com.altratek.altraserver.message.response.RLMessage;
import com.altratek.altraserver.message.response.UserGoneMessage;

public class SystemHandler implements IServerEventHandler {
	private final AltraServer server;
	private final ServerWriter serverWriter;

	public SystemHandler() {
		this.server = AltraServer.getInstance();
		this.serverWriter = this.server.getServerWriter();
	}

	@Override
	public void handleEvent(User user, Object eventData) {
		RequestEvent reqEvent = (RequestEvent) eventData;
		SocketChannel socketChannel = reqEvent.getSenderChannel();
		short action = -1;
		try {
			action = reqEvent.getAction();

			if (!this.checkActionUser(action, user)) {
				this.logUnLoginMessage(socketChannel, action);
				this.server.lostInvalidMsgConn(user, socketChannel, "unlogin sys req");
				return;
			}

			RequestMessage msg = RequestMessage.create(action, reqEvent.getBuffer());
			if (msg == null || !msg.validateMsg()) {
				this.server.lostInvalidMsgConn(user, socketChannel, "sys req msg");
				return;
			}

			int fromRoom = msg.fromRoom;

			if (ServerLogger.debugEnabled) {
				ServerLogger.debugf("sys req : act=%s, user=%s", action, user == null ? "null" : user.getName());
			}

			switch (action) {
			case ReqMsgType.AsObj:
				handleASObject(msg, fromRoom, user);
				break;
			case ReqMsgType.Hit:
				handleUpdateUserOperationTime(user);
				break;
			case ReqMsgType.JoinRoom:
				handleJoinRoom(msg, fromRoom, user);
				break;
			case ReqMsgType.LeaveRoom:
				handleLeaveRoom(msg, fromRoom, user);
				break;
			case ReqMsgType.Login:
				handleLogin(msg, socketChannel);
				break;
			case ReqMsgType.PubMsg:
				handlePublicMessage(msg, fromRoom, user);
				break;
			case ReqMsgType.SetRvars:
				handleSetRoomVariable(msg, fromRoom, user, false);
				break;
			case ReqMsgType.SetUvars:
				handleSetUserVariable(msg, fromRoom, user);
				break;
			default:
				refuseAction(socketChannel, action);
				break;
			}
		} catch (Exception e) {
			ServerLogger.error("<<SystemHandler ProcessEvent>>: Unexpected Exception - ", e);
		}
	}

	private boolean checkActionUser(int reqMsgType, User user) {
		// 登录不要求有user
		// 其他所有命令要求有user和zone，即要求登录zone的用户
		if (reqMsgType == ReqMsgType.Login) {
			return true;
		}
		if (user == null) {
			return false;
		}
		if (user.lost) {
			return false;
		}
		if (user.getZone() == null) {
			return false;
		}
		return true;
	}

	private void handleLogin(RequestMessage msg, SocketChannel sc) {
		LoginMessage loginMsg = (LoginMessage) msg;
		String zoneName = loginMsg.zone;
		String userName = loginMsg.nick;
		String password = loginMsg.pwd;

		String errMsg = "";
		// 此处判断zone是否null，是为了下面方法isCustomLogin()的执行
		Zone targetZone = ZoneManager.instance.getZoneByName(zoneName);
		// 目标Zone是否存在
		if (targetZone == null) {
			errMsg = ConfigData.LOGIN_ZONE_NOTEXIST;
			sendLoginErrorMsg(errMsg, zoneName, sc, true);
			return;
		}

		if (userName == null) {
			return;
		}

		if (password == null)
			password = "";

		// 是否有空位登录
		if (!targetZone.hasPlaceToLogin()) {
			errMsg = ConfigData.LOGIN_ZONE_FULL;
			sendLoginErrorMsg(errMsg, zoneName, sc, false);
			return;
		}

		SystemEventDispatcher.instance.triggerUserLogin(userName, password, targetZone, sc);
	}

	private void sendLoginErrorMsg(String errMsg, String zoneName, SocketChannel recipient, boolean logError) {
		ResponseMessage response = new LogKOMessage(errMsg, recipient);
		serverWriter.sendOutMsg(response);

		if (logError) {
			ServerLogger.error(logErrorDetailMsg(errMsg, zoneName, recipient));
		} else {
			if (ServerLogger.debugEnabled) {
				// duplicated code : lazy eval log message.
				ServerLogger.debug(logErrorDetailMsg(errMsg, zoneName, recipient));
			}
		}
	}

	private String logErrorDetailMsg(String reason, String zoneName, SocketChannel recipient) {
		String ip = this.server.getIpBySocketChannel(recipient);
		return String.format("login error : %s zone[%s], ip[%s]", reason, zoneName, ip);
	}

	/**
	 * 处理获取房间列表操作，返回用户所在区域内的所有房间信息（内容简化版本）
	 */
	@SuppressWarnings("unused")
	private void handleGetRoomList_Short(User user) {
		Zone targetZone = user.getZone();
		ResponseMessage response = new RLMessage(targetZone.getRoomList(), targetZone.getVarsOnRoomList(), user);
		serverWriter.sendOutMsg(response);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("Method[handleGetRoomList_Short] - RoomList sent to User[%s]", user.getName());
		}
	}

	private void handleJoinRoom(RequestMessage msg, int fromRoomId, User user) {
		JoinRoomMessage jrMsg = (JoinRoomMessage) msg;
		try {
			String targetRoomName = jrMsg.targetRoomName;
			boolean leaveRoom = jrMsg.leaveOldRoom;
			int oldRoomId = jrMsg.oldRoomId;
			boolean ignoreMaxCount = jrMsg.ignoreMaxCount;
			String customProperty = jrMsg.customProp;
			Zone currentZone = user.getZone();
			Room oldRoom = currentZone.getRoomById(oldRoomId);
			// 过滤并设定参数后，开始加入房间的流程
			Room targetRoom = currentZone.getRoomByName(targetRoomName);
			if (targetRoom == null) {
				Room fromRoom = user.getInRoom();
				ServerLogger.errorf("join room request error : join a non-existent room[%s], user[%s], fromRoom[%s]",
						targetRoomName, user.getName(), fromRoom != null ? fromRoom.getName() : "null");
				return;
			}

			currentZone.roomManager.joinAndLeaveRoom(user, oldRoom, targetRoom, leaveRoom, ignoreMaxCount,
					customProperty, true, true);
		} catch (JoinRoomException jre) {
			// the exception has been handled in RoomManager, 为了不污染log, ignore it here
		} catch (Exception e) {
			ServerLogger.errorf("<<JoinRoom Request>>: Exception - User[%s]", e, user.getName());
		}
	}

	/**
	 * 处理用户离开房间操作，成功则发送离开房间成功消息，从该房间中删除用户，并删除满足：<br/> 1.人数为0且为临时游戏房间<br/> 2.人数为0且为临时非游戏房间且但创建者已不在线 的房间
	 * 
	 */
	private void handleLeaveRoom(RequestMessage msg, int fromRoomId, User user) {
		Zone zone = user.getZone();
		LeaveRoomMessage lrMsg = (LeaveRoomMessage) msg;
		int leaveRoomId = lrMsg.leaveRoomId;
		zone.roomManager.leaveRoom(user, leaveRoomId, fromRoomId, true);
	}

	/**
	 * 通知房间内其他用户，某个用户离开房间的消息（Room的removeUser方法调用，以及ExtensionHelper对应方法）
	 */
	public void notifyUserLeave(int userId, int roomId, Zone zone) {
		Room room = zone.getRoomById(roomId);

		if (room != null && !room.empty()) {
			List<SocketChannel> recipients = room.getUserSockets();
			ResponseMessage response = new UserGoneMessage(roomId, userId, recipients);
			serverWriter.sendOutMsg(response);

			if (ServerLogger.debugEnabled) {
				ServerLogger.debug("Method[notifyUserLeave] - Response sent to roommate(s)");
			}
		}
	}

	private void handleSetUserVariable(RequestMessage msg, int fromRoomId, User user) {
		SetUvarsMessage suMsg = (SetUvarsMessage) msg;
		if (suMsg.userVariables == null) {
			return;
		}

		user.updateOperationTime();

		HashMap<String, UserVariable> updatedUserVariable = user.setUserVariables(suMsg.userVariables, true, false,
				true);

		if (updatedUserVariable.size() > 0) {
			Room fromRoom = user.getZone().getRoomById(fromRoomId);
			SystemEventDispatcher.instance.triggerUserVariable(user, fromRoom, updatedUserVariable);
		}
	}

	/**
	 * 处理设置房间变量操作
	 * 
	 */
	private void handleSetRoomVariable(RequestMessage msg, int fromRoomId, User user, boolean createRoom) {
		SetRvarsMessage srvMsg = (SetRvarsMessage) msg;
		boolean setOwner = srvMsg.setOwner;

		if (createRoom || user.inRoom(fromRoomId))// 新建房间过程中 or 用户（创建者）在房间内
		{
			Zone zone = user.getZone();
			Room room = zone.getRoomById(fromRoomId);
			if (room != null) {
				user.updateOperationTime();
				HashMap<String, RoomVariable> updatedRoomVariable = new HashMap<String, RoomVariable>();
				for (Entry<String, RoomVariable> entry : srvMsg.newRoomVariables.entrySet()) {
					String varName = entry.getKey();
					RoomVariable rv = entry.getValue();

					if (room.doSetRoomVariable(varName, rv.getType(), rv.getValue(), rv.isPrivate(), rv.isPersistent(),
							user, setOwner)) {
						updatedRoomVariable.put(varName, rv);
					}
				}
				if (updatedRoomVariable.size() > 0)// 成功更新过某个房间变量
				{
					MsgSender.sendRoomVariablesUpdate(room, updatedRoomVariable);

					SystemEventDispatcher.instance.triggerRoomVariable(user, room, updatedRoomVariable);
				}
			}
		}
	}

	private void handleASObject(RequestMessage msg, int roomId, User user) {
		AsObjMessage aoMsg = (AsObjMessage) msg;

		if (!user.inRoom(roomId))
			return;
		user.updateOperationTime();

		Zone zone = user.getZone();
		Room room = zone.getRoomById(roomId);
		if (room != null) {
			List<SocketChannel> allButMe = room.getUserSocketsButOne(user);
			doSendASObject(roomId, user.getSocketChannel(), user, aoMsg.data, allButMe);// 不向拥有者发送（返回）AS对象
			if (ServerLogger.debugEnabled) {
				ServerLogger.debug("Method[handleASObject] - ASObj sent to all roommate(s)");
			}
		} else {
			ServerLogger.errorf("<<ASObject Request>>: Came from for a nonexistent room - IP[%s]", user.getIP());
		}
	}

	private void doSendASObject(int roomId, SocketChannel sc, User user, String dataObj, List<SocketChannel> recipients) {
		ResponseMessage response = new DataObjMessage(roomId, user.getUserId(), dataObj, recipients);
		serverWriter.sendOutMsg(response);
	}

	/**
	 * 处理公开发言操作
	 */
	private void handlePublicMessage(RequestMessage msg, int roomId, User user) {
		PubMsgMessage pubMsgObj = (PubMsgMessage) msg;
		String pubMsg = pubMsgObj.pubMsg;
		if (pubMsg == null) {
			return;
		}
		Zone zone = user.getZone();
		Room room = zone.getRoomById(roomId);
		if (room != null) {
			if (user.inRoom(roomId)) {
				user.updateOperationTime();

				// 是否交由扩展处理
				if (zone.isPubMsgInternalEventEnabled()) {
					SystemEventDispatcher.instance.triggerPublicMessage(pubMsg, user, room, zone);
				} else {
					dispatchPublicMessage(pubMsg, room, user);
				}
			}
		} else {
			ServerLogger.errorf("<<PublicMessage Request>>: The Room[id=%s] is not exist - User[%s] - IP[%s]", roomId,
					user.getName(), user.getIP());
		}
	}

	public void dispatchPublicMessage(String msg, Room room, User user) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("Method[dispatchPublicMessage] - User[%s] said: %s", user.getName(), msg);
		}
		MsgSender.sendPublicMessage(msg, room, user.getUserId());
	}

	private void handleUpdateUserOperationTime(User user) {
		user.updateOperationTime();
	}

	private void logUnLoginMessage(SocketChannel sc, short action) {
		String ip = this.server.getIpBySocketChannel(sc);
		StringBuilder sb = new StringBuilder("<<").append(action);
		sb.append(" Request>>: Comming from a un-logined user - IP[").append(ip).append("]");
		ServerLogger.error(sb.toString());
	}

	private void refuseAction(SocketChannel sc, int action) {
		String ip = this.server.getIpBySocketChannel(sc);
		StringBuilder sb = new StringBuilder("<<SystemHandler>>: Refused Action[").append(action);
		sb.append("] - IP[").append(ip).append("]");
		ServerLogger.error(sb.toString());
	}
}
