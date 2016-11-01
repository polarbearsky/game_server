package com.altratek.altraserver;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.exception.InvalidMsgException;
import com.altratek.altraserver.handler.InMsgHandlerWorkPool;
import com.altratek.altraserver.lib.IServerEventHandler;
import com.altratek.altraserver.lib.RequestEvent;
import com.altratek.altraserver.lib.ServerEvent;
import com.altratek.altraserver.message.MsgConstants;

// �ɷ���Ϣ�������̵߳������(facade)��
// ��ҪĿ���Ƿ�װ�Ƿ�����ϵͳ�̺߳���չ�̵߳��ж��߼�
public final class InMsgHandleFacade {

	private static InMsgHandlerWorkPool sysWorkerPool;
	private static InMsgHandlerWorkPool extWorkerPool;
	private static IServerEventHandler sysHandler;
	private static IServerEventHandler exthandler;

	public static boolean enableSystemThread = true;
	
	static void init() {
		enableSystemThread = ConfigData.ENABLE_SYS_THREAD;
		
		if (enableSystemThread) {
			sysWorkerPool = new InMsgHandlerWorkPool("SysHandler", ConfigData.ST_HANDLER_THREADS);
			extWorkerPool = new InMsgHandlerWorkPool("ExtHandler", ConfigData.XT_HANDLER_THREADS);
		} else {
			// ������ϵͳ�̺߳���չ�̣߳�������Ϣ��ͳһһ���̳߳ش���
			// Ϊ�˼��������ط���if(enableSystemThread)�������������̳߳ر�������ָ�Ķ���ͳһ�̳߳ض���
			int totalThreadCount = ConfigData.XT_HANDLER_THREADS + ConfigData.ST_HANDLER_THREADS;
			extWorkerPool = new InMsgHandlerWorkPool("MsgHandler", totalThreadCount);
			sysWorkerPool = extWorkerPool;
		}

		sysHandler = AltraServer.getInstance().getSystemHandler();
		exthandler = AltraServer.getInstance().getExtensionHandler();
	}

	static int systemHandlerThreadCount() {
		return sysWorkerPool.workerCount;
	}

	static int extensionHandlerThreadCount() {
		return extWorkerPool.workerCount;
	}

	static void acceptEvent(RequestEvent reqEvent, Session session, User user) throws InvalidMsgException {		
		switch (reqEvent.getType()) {
		case MsgConstants.MSGTYPE_SYSTEM:
			sysWorkerPool.acceptEvent(new ServerEvent(user, reqEvent, sysHandler), session.sysDistributeKey);
			break;
		case MsgConstants.MSGTYPE_EXTENSION:
			extWorkerPool.acceptEvent(new ServerEvent(user, reqEvent, exthandler), session.extDistributeKey);
			break;
		default:
			throw new InvalidMsgException();
		}
	}

	static void shutdown() throws InterruptedException {
		if (enableSystemThread) {
			sysWorkerPool.shutdown();
		}
		extWorkerPool.shutdown();
	}
}
