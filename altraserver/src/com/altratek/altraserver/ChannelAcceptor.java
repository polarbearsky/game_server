package com.altratek.altraserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.AltraServerUtils;

class ChannelAcceptor extends Thread {
	private volatile boolean running = true;
	private ServerSocketChannel serverSocketChannel;
	private Selector acceptSelector;

	private final CopyAndClearList<SocketChannel> newChannels = new CopyAndClearList<SocketChannel>();

	void init() throws IOException {
		this.setName("Acceptor");

		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		// setReuseAddress应在bind之前调用才有效,其作用为:允许绑定处于TIME_WAIT 状态的端口
		serverSocketChannel.socket().setReuseAddress(true);
		serverSocketChannel.socket().bind(new InetSocketAddress(ConfigData.SERVER_PORT));

		acceptSelector = Selector.open();
		serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
	}

	List<SocketChannel> getNewChannels() {
		return newChannels.copy();
	}

	private void setNewConns(List<SocketChannel> channelList) {
		this.newChannels.addAll(channelList);
		// AltraServer.getInstance().newChannelsReady();
	}

	@Override
	public void run() {
		while (running) {
			try {
				this.acceptNewChannels();
			} catch (Throwable t) {
				ServerLogger.error("accept error : ", t);
			}
		}
		ServerLogger.info("acceptor closed.");
	}

	private void acceptNewChannels() {
		List<SocketChannel> channelList = new LinkedList<SocketChannel>();
		try {
			acceptSelector.select();
			Set<SelectionKey> readyKeys = acceptSelector.selectedKeys();
			for (Iterator<SelectionKey> it = readyKeys.iterator(); it.hasNext();) {
				SelectionKey sk = it.next();
				it.remove();
				this.acceptOneChannel(channelList, sk);
			}
		} catch (ClosedSelectorException e) {
			// shutdown触发，正常
			if (running) {
				ServerLogger.error("acceptNewChannels error : ", e);
			}
		} catch (Exception e) {
			ServerLogger.error("acceptNewChannels error : ", e);
		}

		this.setNewConns(channelList);
	}

	private void acceptOneChannel(List<SocketChannel> channelList, SelectionKey sk) {
		try {
			ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
			SocketChannel sc = ssc.accept();
			if (!IpFloodChecker.instance.addIp(sc)) {
				ServerLogger.errorf("Too many connections from IP[%s]", AltraServerUtils.getIpBySocketChannel(sc));
				ChannelManager.instance.closeSocketChannel(sc);
				return;
			}

			channelList.add(sc);

			if (ServerLogger.debugEnabled) {
				ServerLogger.debugf("Accept channel from IP[%s]", AltraServerUtils.getIpBySocketChannel(sc));
			}
		} catch (Exception e) {
			ServerLogger.error("acceptOneChannel error : ", e);
		}
	}

	void shutdown() {
		this.running = false;
		
		this.getNewChannels(); // get出来相当于扔了
		
		try {
			this.acceptSelector.wakeup();
			this.acceptSelector.close();
		} catch (IOException ignore) {
		}

		try {
			this.join();
		} catch (InterruptedException ignore) {
		}
	}

	void closeServerSocket() throws IOException {
		serverSocketChannel.socket().close();
		serverSocketChannel.close();
		serverSocketChannel = null;
	}
}
