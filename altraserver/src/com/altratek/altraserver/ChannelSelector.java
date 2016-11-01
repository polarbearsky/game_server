package com.altratek.altraserver;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.exception.InvalidMsgException;
import com.altratek.altraserver.lib.RequestEvent;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.response.CrossDomainMessage;
import com.altratek.altraserver.util.AltraServerUtils;
import com.altratek.altraserver.util.ServerStat;

class ChannelSelector extends Thread {
	private final static int MSG_HEAD_LEN = 4; // bytes of int
	private final static long CHECK_UNLOGIN_INTERVAL = 10000; // 10 seconds

	private volatile boolean running = true;
	private Selector readSelector;
	private long lastCheckUnLoginTime = 0;
	private AltraServer server;
	private ChannelAcceptor channelAcceptor;
	private ServerWriter serverWriter;
	private String crossDomainContent;

	void init(ChannelAcceptor channelAcceptor) throws IOException {
		readSelector = Selector.open();
		this.server = AltraServer.getInstance();
		this.channelAcceptor = channelAcceptor;
		this.serverWriter = this.server.getServerWriter();
		this.setName("Selector");
		this.buildCrossDomainPolicyString();
		this.setUncaughtExceptionHandler(ThreadUncaughtExceptionHandler.getHandler());
	}

	private void buildCrossDomainPolicyString() {
		if (!ConfigData.EXTERNAL_CROSS_DOAMIN) {
			StringBuilder sb = new StringBuilder("<cross-domain-policy>");
			if (ConfigData.ALLOWED_DOMAINS != null && ConfigData.ALLOWED_DOMAINS.size() > 0) {
				for (int i = 0; i < ConfigData.ALLOWED_DOMAINS.size(); i++) {
					sb.append("<allow-access-from domain='");
					sb.append(ConfigData.ALLOWED_DOMAINS.get(i));
					sb.append("' to-ports='");
					sb.append(ConfigData.CROSS_DOMAIN_PORT);
					sb.append("'/>");
				}
				sb.append("</cross-domain-policy>");
			} else {
				sb.append("<allow-access-from domain=\"*\" to-ports=\"");
				sb.append(ConfigData.CROSS_DOMAIN_PORT).append("\" /></cross-domain-policy>");
			}
			crossDomainContent = sb.toString();
		}
	}

	private void sendCrossDomainContent(SocketChannel sc) {
		if (!ConfigData.AUTOSEND_CROSS_DOMAIN) {
			return;
		}

		serverWriter.sendOutMsg(new CrossDomainMessage(crossDomainContent, sc));

		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("CrossDomainContent sent");
		}
	}

	@Override
	public void run() {
		this.lastCheckUnLoginTime = System.currentTimeMillis();
		while (this.running) {
			try {
				this.checkUnLoginChannels();

				ChannelManager.instance.lostWillLostConns();

				this.registerNewChannels();
				this.select();
				// Thread.sleep(5L);
				// ������ﲻsleep + selectNow��mirror GC 2s һ��
				// ������select(timeout)�滻selectNow()
			} catch (Throwable t) {
				ServerLogger.error("ChannelSelector run error : ", t);
			}
		}
		ServerLogger.info("selector closed.");
	}

	// void wakeupSelect() {
	// this.readSelector.wakeup();
	// }

	private void checkUnLoginChannels() {
		long now = System.currentTimeMillis();
		long timeDiff = now - lastCheckUnLoginTime;
		if (timeDiff >= CHECK_UNLOGIN_INTERVAL) {
			this.lastCheckUnLoginTime = now;
			ChannelManager.instance.checkUnLoginChannels();
		}
	}

	private void registerNewChannels() {
		List<SocketChannel> toAddNewConns = this.channelAcceptor.getNewChannels();
		if (toAddNewConns == null) {
			return;
		}

		for (SocketChannel sc : toAddNewConns) {
			try {
				if (sc != null) {
					ChannelManager.instance.addChannel(sc);

					this.registerChannel(sc);

					sendCrossDomainContent(sc);
				}

				if (ServerLogger.debugEnabled) {
					ServerLogger.debugf("Start reading msg from IP[%s]", AltraServerUtils.getIpBySocketChannel(sc));
				}
			} catch (Exception e) {
				ServerLogger.error("registerNewChannels error : ", e);
			}
		}
	}

	private void registerChannel(SocketChannel sc) throws IOException {
		sc.configureBlocking(false);
		//sc.socket().setTcpNoDelay(true);
		Session session = new Session(sc);
		SelectionKey sk = sc.register(readSelector, SelectionKey.OP_READ, session);
		session.selectionKey = sk;
	}

	void select() {
		try {
			this.doSelect();
		} catch (IOException ioe) {
			ServerLogger.error("select : I/O problems in reading socket - ", ioe);
		} catch (Throwable t) {
			ServerLogger.error("select : Throwable - ", t);
		}
	}

	private void doSelect() throws Exception {
		// �����select()���޳�ʱ���߳�����������û�����registerNewChannels��ע��������
		// ��������취��
		// 1. ���������ӽ���ʱ��wakeup������select����, see wakeupSelect()
		// 2. ����select timeout������timeoutʱ�䣬������Ȼͨ��
		// Ŀǰ���ð취����ԭ�򣬼�һ�㡣
		int selectedKeyCount = readSelector.select(10L);
		if (selectedKeyCount == 0) {
			return;
		}

		Set<SelectionKey> readyKeys = readSelector.selectedKeys();
		for (Iterator<SelectionKey> it = readyKeys.iterator(); it.hasNext();) {
			SelectionKey sk = it.next();
			it.remove();

			if (!sk.isValid()) {
				continue;
			}

			SocketChannel sc = (SocketChannel) sk.channel();
			Session session = (Session) sk.attachment();

			if (session.lost) {
				continue;
			}

			if (sk.isWritable()) {
				handleWrite(sk, sc, session);
			} else {
				handleRead(sk, sc, session);
			}
		}
	}

	private void handleWrite(SelectionKey sk, SocketChannel sc, Session session) {
		sk.interestOps(SelectionKey.OP_READ);
		this.serverWriter.dispatchMsgChannelToWorker(session);
	}

	private void handleRead(SelectionKey sk, SocketChannel sc, Session session) {
		User user = session.user; // ��¼��user������attachment

		try {
			if (!readBytesFromChannel(sc, session.inMsgBuffer)) {
				lostConn(sc, "read data failed");
				return;
			}

			// ������Ȳ���4���ֽڣ���λ����Ϊ���ޣ���������Ϊ�����������ȴ������㹻������
			if (session.inMsgBuffer.remaining() < 4) {
				session.inMsgBuffer.position(session.inMsgBuffer.limit()); // �Ƶ����һ��λ�ã�׼��ӭ��������
				session.inMsgBuffer.limit(session.inMsgBuffer.capacity());
				return;
			}

			int size = session.inMsgBuffer.getInt();
			boolean hasSizeBytes = true;
			while (session.inMsgBuffer.remaining() >= size) {

				hasSizeBytes = false; // ��ȡһ����Ϣ���޷�ȷ�ϻ���������������false

				if (!validateMsgBytes(size, user, session)) {
					this.server.lostInvalidMsgConn(user, sc, "seq no");
					return;
				}

				this.demultiplexAndDispatch(size, session, sc, user);

				this.logMsgBytesLength(size);

				// ���ʣ���ֽ���������ʾ��һ����Ϣ�ĳ��ȣ��򽫻�������ʣ����ֽ���ǰ�ƶ���ɾȥ�Ѿ���ȡ�����ݣ�
				if (session.inMsgBuffer.remaining() < 4) {
					session.inMsgBuffer.compact();
					break;
				} else {
					size = session.inMsgBuffer.getInt();
					hasSizeBytes = true;
				}
			}

			// ִ�е�����˵����size����û���㹻size�ĺ���bytes
			if (hasSizeBytes) {
				// λ�û�������Ϣ�������ڵ�һ���ֽ�
				session.inMsgBuffer.position(session.inMsgBuffer.position() - 4);
				// ɾ���Ѿ���ȡ������
				session.inMsgBuffer.compact();
			}
		} catch (Exception e) {
			if (e instanceof InvalidMsgException || e instanceof BufferUnderflowException) {
				ServerLogger.invalidMsg(user, "select read");
			}
			lostConn(sc, "read unknow error");
			ServerLogger.error("<<Read Client Message>>: Exception - ", e);
		}
	}

	private boolean readBytesFromChannel(SocketChannel sc, ByteBuffer msgBuffer) throws Exception {
		try {
			// ��ȡ�����ֽ���Ϊ-1��˵���ͻ�������Ҫ��ر�����
			if (sc.read(msgBuffer) == -1) {
				return false;
			}
		} catch (IOException e) {
			// ����쳣�������ģ����Բ�log error��̫����
			// debugEnabled��¼error���ǹ���ģ��Ǳ���
			if (ServerLogger.debugEnabled) {
				ServerLogger.error("readBytesFromChannel error", e);
			}
			return false;
		}

		msgBuffer.flip();

		return true;
	}

	private boolean validateMsgBytes(int size, User user, Session session) {
		// ����Ա��Ϣ��size�������⴦����ʵ����Ա�İ�Ŀǰ����С��û�з�����Ϣ��
		if (size <= 0 || size > ConfigData.MAX_MSG_BYTE_LEN) {
			ServerLogger.error("Buffer size error: " + size);
			return false;
		}

		// check seq no
		if (!session.checkMsgSeqNo()) {
			// Ϊ�˱���ÿ���޸�������Ϣ����Ų��ԣ���Ҫ�޸Ĺ������ͻ���alsadmin.jar
			// �İ���ţ����Թ���Ա����֤����ţ�����Ա�İ�������ֶΣ���ֵ����0�����仯
			// ����¼��Ϣ�޷�ʶ���ǹ���Ա�����Թ涨��¼��Ϣ�����(��һ����)�̶���0��
			// Ϊʲô��֤ʧ�ܵ�ʱ��ż�����Ա����أ�����������Ӧ�������ж�����Ա��
			// ��������ǹ���Ա������֤��Ϣ���кš�
			// ��Ϊ����Ա��ݺ��ٵ�¼��ֻ����ά���ط����reload����ʹ�ã�����û�й���Ա��Ϣ
			// ��û�б�ҪΪ���⼸��û�е���Ϣÿ�ζ�ȥ������Ա�жϡ�
			// ��Ϊ��־��checkMsgSeqNo���ˣ�����Ա��Ϣ�д�����־������Ӱ��������С�
			boolean isAdmin = user != null && user.isAdmin();
			return isAdmin ? true : false;
		}

		return true;
	}

	// �������Ϣ���ɷ�
	private void demultiplexAndDispatch(int size, Session session, SocketChannel sc, User user)
			throws InvalidMsgException {
		final int netSize = size - MSG_HEAD_LEN;
		// too many copys ?
		byte[] temp = new byte[netSize];
		session.inMsgBuffer.get(temp); // it is the only copy
		int msgSeqNo = session.getMsgSeqNo();
		// Ϊ�˼��admin client�Ĳ����ԣ�msg seq no=0����Ϣ��������
		// admin client��msg seq no always 0
		if (msgSeqNo != 0) {
			this.server.getMessageValidator().decryptMsg(temp, msgSeqNo);
		}
		// why wrap instead of put ? wrap no copy.
		IoBuffer buffer = IoBuffer.wrap(temp);
		// no flip here
		// NOTE : why wrapped new buffer can't be flipped ? probably 2 flips.
		// What if you flip a buffer twice? It effectively becomes zero-sized.
		// see <java nio> chapter2 buffers

		dispatchEvent(buffer, sc, user, session);
	}

	private void logMsgBytesLength(int size) {
		int inMsgLength = size + 4; // 4 is msg length int
		ServerStat.instance.increaseInBytes(inMsgLength);
		if (ConfigData.ENABLE_MSG_LENGTH_DEBUG) {
			ServerLogger.msgLengthDebug(String.format("in/%s", inMsgLength));
		}
	}

	private void dispatchEvent(IoBuffer buffer, SocketChannel sc, User user, Session session)
			throws InvalidMsgException {
		if (server.shutdowning) {
			return;
		}

		if (buffer == null) {
			return;
		}

		RequestEvent reqEvent = new RequestEvent(buffer, sc);

		InMsgHandleFacade.acceptEvent(reqEvent, session, user);
	}

	SelectionKey getChannelKey(SocketChannel sc) {
		if (sc == null) {
			return null;
		}
		// keyFor��һ��˳����ң�����һ��SocketChannelֻע����һ��selector
		// һ��Ԫ�������˳����ң�û�������⡣
		return sc.keyFor(this.readSelector);
	}

	void lostConn(SocketChannel sc, String userLostParam) {
		this.server.lostConn(sc, userLostParam);
	}

	void shutdown() {
		this.running = false;

		try {
			this.readSelector.wakeup();
			this.readSelector.close();
		} catch (IOException ignore) {
		}

		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}
}