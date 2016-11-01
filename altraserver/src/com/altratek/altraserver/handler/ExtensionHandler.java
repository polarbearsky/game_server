package com.altratek.altraserver.handler;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.extensions.ExtensionManager;
import com.altratek.altraserver.extensions.IAltraExtension;
import com.altratek.altraserver.lib.ActionscriptObject;
import com.altratek.altraserver.lib.IServerEventHandler;
import com.altratek.altraserver.lib.RequestEvent;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.ReqMsgType;
import com.altratek.altraserver.message.request.RequestMessage;
import com.altratek.altraserver.message.request.XtReqMessage;

public class ExtensionHandler implements IServerEventHandler {
	private final AltraServer server;
	private final Zone adminZone;

	public ExtensionHandler() {
		this.server = AltraServer.getInstance();
		adminZone = Zone.adminZone;
	}

	public void init() {
		ExtensionManager.loadAdminExtension();// server启动时执行，加载管理员扩展
	}

	@Override
	public void handleEvent(User user, Object extReqEvent) {
		RequestEvent reqEvent = (RequestEvent) extReqEvent;
		try {
			SocketChannel senderChannel = reqEvent.getSenderChannel();
			if (user == null) {
				ServerLogger.errorf("Ext Handler : an un-logined request - IP[%s]", server
						.getIpBySocketChannel(senderChannel));
				this.server.lostInvalidMsgConn(user, senderChannel, "unlogin ext req");
				return;
			}

			if (user.lost) {
				return;
			}

			short action = reqEvent.getAction();
			switch (action) {
			case ReqMsgType.XtReq:
				this.handleObjectReq(reqEvent, user);
				break;
			case ReqMsgType.XtReqB:
				this.handleByteReq(reqEvent, user);
				break;
			default:
				ServerLogger.error("Ext Handler : unknow action, conn will be lost");
				this.server.lostInvalidMsgConn(user, senderChannel, "ext msg head");
				return;
			}
		} catch (Exception e) {
			ServerLogger.error("<<Extension Handler>> error : ", e);
		}
	}

	private void handleObjectReq(RequestEvent reqEvent, User user) {
		XtReqMessage xrMsg = (XtReqMessage) RequestMessage.create(ReqMsgType.XtReq, reqEvent.getBuffer());
		if (xrMsg == null || !xrMsg.validateMsg()) {
			this.server.lostInvalidMsgConn(user, user.getSocketChannel(), "ext msg");
			return;
		}

		int fromRoom = xrMsg.fromRoom;
		int extensionId = xrMsg.extenionId;
		String xtCmd = xrMsg.cmd;
		ActionscriptObject param = xrMsg.paramObj;

		Zone zone = extensionId == ConfigData.ADMIN_EXT_ID ? adminZone : user.getZone();
		IAltraExtension ae = zone.extensionManager.getExtension(extensionId);
		if (ae == null) {
			ServerLogger.errorf("can't find extension by ext id [%s], from cmd [%s]", extensionId, xtCmd);
			// this.server.lostInvalidMsgConn(user, user.getSocketChannel(), "wrong ext id");
			return;
		}

		user.updateOperationTime();

		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("==> %s, cmd=%s, user=%s, param=%s", extensionId, xtCmd, user.getName(), param);
		}

		long beginTime = System.currentTimeMillis();

		ae.handleRequest(xtCmd, param, user, fromRoom);

		if (ConfigData.ENABLE_PROFILE) {
			ServerLogger.prof(extensionId, xtCmd, beginTime);
		}
	}

	private void handleByteReq(RequestEvent reqEvent, User user) {
		IoBuffer buffer = reqEvent.getBuffer();
		// XtReqMessageB xrMsg = new XtReqMessageB(buffer);
		// int fromRoom = xrMsg.fromRoom;
		// 以上两行代码中，用XtReqMessageB对象，只是为了获取一个fromRoom
		// 因为这里调用频繁，为了效率，直接用buffer获取fromRoom，牺牲了封装性
		int fromRoom = buffer.getInt();
		if (buffer.remaining() > 0) {
			byte xtCmd = buffer.get();
			IAltraExtension ae = user.getZone().extensionManager.getExtension(xtCmd);
			if (ae == null) {
				ServerLogger.errorf("can't find extension by byte cmd [%s]", xtCmd);
				this.server.lostInvalidMsgConn(user, user.getSocketChannel(), "wrong ext byte cmd");
				return;
			}

			user.updateOperationTime();

			long beginTime = System.currentTimeMillis();

			ae.handleRequest(xtCmd, buffer, user, fromRoom);

			if (ConfigData.ENABLE_PROFILE) {
				ServerLogger.prof("bCmd", xtCmd, beginTime);
			}
		} else {
			this.server.lostInvalidMsgConn(user, user.getSocketChannel(), "ext byte msg");
		}
	}
}