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
				// 如果这里不sleep + selectNow，mirror GC 2s 一次
				// 所以用select(timeout)替换selectNow()
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
		// 如果用select()，无超时，线程阻塞，可能没机会调registerNewChannels来注册新链接
		// 两个解决办法：
		// 1. 当有新链接进来时，wakeup阻塞的select操作, see wakeupSelect()
		// 2. 设置select timeout，过了timeout时间，阻塞自然通了
		// 目前采用办法二，原因，简单一点。
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
		User user = session.user; // 登录后，user被放入attachment

		try {
			if (!readBytesFromChannel(sc, session.inMsgBuffer)) {
				lostConn(sc, "read data failed");
				return;
			}

			// 如果长度不足4个字节，则将位置置为极限，将极限置为容量，继续等待至有足够的数据
			if (session.inMsgBuffer.remaining() < 4) {
				session.inMsgBuffer.position(session.inMsgBuffer.limit()); // 移到最后一个位置，准备迎接新数据
				session.inMsgBuffer.limit(session.inMsgBuffer.capacity());
				return;
			}

			int size = session.inMsgBuffer.getInt();
			boolean hasSizeBytes = true;
			while (session.inMsgBuffer.remaining() >= size) {

				hasSizeBytes = false; // 读取一条消息后，无法确认还有下条，所以先false

				if (!validateMsgBytes(size, user, session)) {
					this.server.lostInvalidMsgConn(user, sc, "seq no");
					return;
				}

				this.demultiplexAndDispatch(size, session, sc, user);

				this.logMsgBytesLength(size);

				// 如果剩余字节数不足显示下一条消息的长度，则将缓冲区内剩余的字节向前移动（删去已经读取的数据）
				if (session.inMsgBuffer.remaining() < 4) {
					session.inMsgBuffer.compact();
					break;
				} else {
					size = session.inMsgBuffer.getInt();
					hasSizeBytes = true;
				}
			}

			// 执行到这里说明有size，但没有足够size的后续bytes
			if (hasSizeBytes) {
				// 位置回退至消息长度所在第一个字节
				session.inMsgBuffer.position(session.inMsgBuffer.position() - 4);
				// 删除已经读取的数据
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
			// 读取到的字节数为-1，说明客户端主动要求关闭连接
			if (sc.read(msgBuffer) == -1) {
				return false;
			}
		} catch (IOException e) {
			// 这个异常是正常的，所以不log error，太多了
			// debugEnabled记录error，是故意的，非笔误
			if (ServerLogger.debugEnabled) {
				ServerLogger.error("readBytesFromChannel error", e);
			}
			return false;
		}

		msgBuffer.flip();

		return true;
	}

	private boolean validateMsgBytes(int size, User user, Session session) {
		// 管理员消息的size不做特殊处理，其实管理员的包目前都很小，没有发大消息的
		if (size <= 0 || size > ConfigData.MAX_MSG_BYTE_LEN) {
			ServerLogger.error("Buffer size error: " + size);
			return false;
		}

		// check seq no
		if (!session.checkMsgSeqNo()) {
			// 为了避免每次修改上行消息包序号策略，都要修改管理程序客户端alsadmin.jar
			// 的包序号，所以管理员不验证包序号，管理员的包有序号字段，但值都是0，不变化
			// 但登录消息无法识别是管理员，所以规定登录消息的序号(第一个包)固定是0。
			// 为什么验证失败的时候才检查管理员身份呢？正常的做法应该是先判定管理员，
			// 如果发现是管理员，不验证消息序列号。
			// 因为管理员身份很少登录，只有运维做关服务和reload配置使用，几乎没有管理员消息
			// ，没有必要为了这几乎没有的消息每次都去做管理员判断。
			// 因为日志在checkMsgSeqNo记了，管理员消息有错误日志，但不影响后续运行。
			boolean isAdmin = user != null && user.isAdmin();
			return isAdmin ? true : false;
		}

		return true;
	}

	// 分离出消息并派发
	private void demultiplexAndDispatch(int size, Session session, SocketChannel sc, User user)
			throws InvalidMsgException {
		final int netSize = size - MSG_HEAD_LEN;
		// too many copys ?
		byte[] temp = new byte[netSize];
		session.inMsgBuffer.get(temp); // it is the only copy
		int msgSeqNo = session.getMsgSeqNo();
		// 为了兼顾admin client的不变性，msg seq no=0的消息都不加密
		// admin client的msg seq no always 0
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
		// keyFor是一个顺序查找，不过一个SocketChannel只注册了一个selector
		// 一个元素数组的顺序查找，没性能问题。
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