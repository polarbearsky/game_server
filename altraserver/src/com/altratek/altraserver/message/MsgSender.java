package com.altratek.altraserver.message;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ChannelManager;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.lib.ActionscriptObject;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.response.AdminMsgMessage;
import com.altratek.altraserver.message.response.PubMsgResopnseMessage;
import com.altratek.altraserver.message.response.RVarsUpdateMessage;
import com.altratek.altraserver.message.response.UERMessage;
import com.altratek.altraserver.message.response.UVarsUpdateMessage;
import com.altratek.altraserver.message.response.XtResMessageB;

// a facade for send reponse message
public final class MsgSender {

	public static void sendXtResponse(ActionscriptObject asObject, MessageChannel recipient, Object caller) {
		sendXtResponse(asObject, toSocketList(recipient), caller, true);
	}

	public static void sendXtResponse(ActionscriptObject asObject, Collection<? extends MessageChannel> recipients, Object caller) {
		sendXtResponse(asObject, toSocketList(recipients), caller, true);
	}

	public static void sendXtResponse(ActionscriptObject asObject, SocketChannel recipient, Object caller) {
		List<SocketChannel> socketList = new ArrayList<SocketChannel>(1);
		socketList.add(recipient);
		sendXtResponse(asObject, socketList, caller, true);
	}

	// dummy参数的作用：
	// 两个同名函数，如果参数仅有List后面<>中的类型参数不同的话，会认为是重复函数，所以加个无用参数区分。
	private static void sendXtResponse(ActionscriptObject asObject, List<SocketChannel> recipients, Object caller,
			boolean dummy) {
		if (recipients.size() == 0) {
			return;
		}

		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("<== %s, recipients size=%s, data=%s", caller.getClass().getSimpleName(), recipients
					.size(), asObject);
		}

		ResponseMessage response = ActionscriptObject.exMessageCodec.createXtResMessage(asObject, recipients);
		sendOutMsg(response);
	}

	public static void sendXtByteResponse(byte[] arrByteCmd, MessageChannel recipient, Object caller) {
		sendXtByteResponse(arrByteCmd, toSocketList(recipient), caller, true);
	}

	public static void sendXtByteResponse(byte[] arrByteCmd, Collection<? extends MessageChannel> recipients, Object caller) {
		sendXtByteResponse(arrByteCmd, toSocketList(recipients), caller, true);
	}

	private static void sendXtByteResponse(byte[] arrByteCmd, List<SocketChannel> recipients, Object caller,
			boolean dummy) {
		if (recipients.size() == 0) {
			return;
		}

		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("<== %s, recipients size=%s, byte data=%s", caller.getClass().getSimpleName(),
					recipients.size(), arrByteCmd);
		}
		ResponseMessage response = new XtResMessageB(arrByteCmd, recipients);
		sendOutMsg(response);
	}

	public static void sendAdminMessage(String message, User recipient) {
		sendAdminMessageToList(message, toSocketList(recipient));
	}

	public static void sendAdminMessageToRoom(String message, Room recipientRoom) {
		sendAdminMessageToList(message, recipientRoom.getUserSockets());
	}

	public static void sendAdminMessageToZone(String message, Zone recipientZone) {
		sendAdminMessageToList(message, recipientZone.getUserChannelList());
	}

	public static void sendAdminMessageToAll(String message) {
		sendAdminMessageToList(message, ChannelManager.instance.getClientChannelClone());
	}

	private static void sendAdminMessageToList(String msg, List<SocketChannel> recipients) {
		ResponseMessage response = new AdminMsgMessage(msg, recipients);
		sendOutMsg(response);
	}

	public static void sendPublicMessage(String msg, Room room, int userId) {
		List<SocketChannel> recipients = room.getUserSockets();
		ResponseMessage response = new PubMsgResopnseMessage(room.getId(), userId, msg, recipients);
		sendOutMsg(response);
	}

	public static void sendRoomVariablesUpdate(Room room, HashMap<String, RoomVariable> roomVars) {
		List<SocketChannel> recipients = room.getUserSockets();
		ResponseMessage response = new RVarsUpdateMessage(room.getId(), roomVars, recipients);
		sendOutMsg(response);
	}

	public static void sendUserVariablesUpdate(User user, Room room, HashMap<String, UserVariable> userVars, boolean notifySelf) {
		List<SocketChannel> recipients = notifySelf ? room.getUserSockets() : room.getUserSocketsButOne(user);
		ResponseMessage response = new UVarsUpdateMessage(room.getId(), user.getUserId(), userVars, recipients);
		sendOutMsg(response);
	}	

	public static void sendUserEnterRoom(int userId, String userName, Map<String, UserVariable> userVar, int targetRoomId,
			List<SocketChannel> recipients, boolean forceIntoStage, String customProperty) {
		ResponseMessage response = new UERMessage(targetRoomId, userId, userName, userVar, forceIntoStage,
				customProperty, recipients);
		sendOutMsg(response);
	}

	private static void sendOutMsg(ResponseMessage response) {
		AltraServer.getInstance().getServerWriter().sendOutMsg(response);
	}

	private static List<SocketChannel> toSocketList(Collection<? extends MessageChannel> recipients) {
		List<SocketChannel> sockets = new ArrayList<SocketChannel>(recipients.size());
		for (MessageChannel c : recipients) {
			sockets.add(c.getSocketChannel());
		}
		return sockets;
	}

	private static List<SocketChannel> toSocketList(MessageChannel recipient) {
		List<SocketChannel> sockets = new ArrayList<SocketChannel>(1);
		sockets.add(recipient.getSocketChannel());
		return sockets;
	}
}
