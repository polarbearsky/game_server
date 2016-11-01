package com.altratek.altraserver.message.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.lib.ActionscriptObject;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgConstants;
import com.altratek.altraserver.message.ResponseMessage;

import flex.messaging.io.amf.Amf3Output;
import flex.messaging.io.amf.AmfObjectFactory;

public class XtResMessage extends ResponseMessage {
	public XtResMessage(String data, List<SocketChannel> recipients) {
		super(MsgConstants.MSGTYPE_EXTENSION, recipients);
		putString(data);
	}

	public XtResMessage(ActionscriptObject ao, List<SocketChannel> recipients) throws IOException {
		super(MsgConstants.MSGTYPE_EXTENSION, recipients);

		Amf3OutputStream outputStream = Amf3OutputStream.create(this.buffer);

		Amf3Output output = AmfObjectFactory.createAmf3Output();
		output.setOutputStream(outputStream);

		output.writeObject(ao);
		output.flush();

		if (ConfigData.ENABLE_MSG_LENGTH_DEBUG) {
			Object cmd = ao.get("_cmd");
			if (cmd != null) {
				// 记扩展命令是用于和长度关联，便于优化消息长度，不记录cmd，
				// 只知道长度，不知道是那个命令发出的。
				final int msgLen = outputStream.buffer.position();
				ServerLogger.msgLengthDebug(String.format("\t%s:%s", cmd, msgLen));
			}
		}

		try {
			if (output != null)
				output.close();
		} catch (IOException ioe) {
			ServerLogger.error("<<XtReqMessage>>: Error occured while closing Amf3Output");
		}

		try {
			if (outputStream != null)
				outputStream.close();
		} catch (IOException ioe) {
			ServerLogger.error("<<XtReqMessage>>: Error occured while closing ByteArrayOutputStream");
		}
	}

	private static class Amf3OutputStream extends OutputStream {
		// thread local解决Amf3OutputStream对象创建代价。
		private static final ThreadLocal<Amf3OutputStream> amf3OutputStreamStore = new ThreadLocal<Amf3OutputStream>() {
			protected Amf3OutputStream initialValue() {
				return new Amf3OutputStream();
			}
		};

		public static Amf3OutputStream create(IoBuffer buffer) {
			Amf3OutputStream o = amf3OutputStreamStore.get();
			o.buffer = buffer;
			return o;
		}

		private IoBuffer buffer;

		@Override
		public void write(int b) throws IOException {
			this.buffer.put((byte) b);
		}

		@Override
		public void write(byte[] src, int offset, int length) {
			this.buffer.put(src, offset, length);
		}

		@Override
		public void close() throws IOException {
			this.buffer = null;
		}
	}
}