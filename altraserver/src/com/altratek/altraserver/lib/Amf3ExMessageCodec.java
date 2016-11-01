package com.altratek.altraserver.lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.AmfObjectFactory;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.request.XtReqMessage;
import com.altratek.altraserver.message.response.XtResMessage;

public class Amf3ExMessageCodec implements IExMessageCodec {
	@SuppressWarnings("unchecked")
	public boolean setXtReqMessageAsObj(XtReqMessage msg, IoBuffer buffer) {
		byte[] reqBytes = null;
		ByteArrayInputStream bais = null;
		Amf3Input input = null;
		try {
			// reqBytes = new byte[buffer.remaining()];
			// buffer.get(reqBytes);
			// no copy, so comment it
			reqBytes = buffer.array();

			bais = new ByteArrayInputStream(reqBytes, buffer.position(), buffer.remaining());

			input = AmfObjectFactory.createAmf3Input();
			input.setInputStream(bais);

			// ����input.readObject()�������ǿͻ���ֱ��ͨ������api��������paramObj
			// sendXtMessage(xtName:String, cmd:String, paramObj:*)
			// һ�㶼��һ��as object (new object() �� {})������ʵ��null��ԭʼ��������Ҳ
			// û�����⣬��������֮���޷���ȡ�ˣ���Ϊû������(key)
			// �����null������input.readObject()��null�����ܺϷ������޷���ȡ���ʶ�����
			Object o = input.readObject();
			if (o instanceof ActionscriptObject) {
				msg.paramObj = (ActionscriptObject) o;
			}

			return true;
		} catch (Exception e) {
			ServerLogger.error("amf3 parse error : ", e);

			return false;
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (Exception ignoreEx) {
			}

			try {
				if (bais != null) {
					bais.close();
				}
			} catch (Exception ignoreEx) {
			}
			reqBytes = null;
		}
	}

	@SuppressWarnings("unused")
	private void logInvalidBytes(byte[] reqBytes) {
		StringBuilder sb = new StringBuilder();
		if (reqBytes != null) {
			sb.append("XtMsgLength = ").append(reqBytes.length).append("\n");
			for (int i = 0; i < reqBytes.length; i++) {
				if (i != 0 && i % 10 == 0) {
					sb.append("\n");
				}
				if ((reqBytes[i] >= 7 && reqBytes[i] <= 10) || reqBytes[i] == 13 || reqBytes[i] < 0) {
					sb.append(String.format("%-7s", new StringBuilder("<").append(reqBytes[i]).append(">").toString()));
				} else {
					sb.append(String.format("%-7s", (char) reqBytes[i]));
				}
			}
		}
		ServerLogger.error(sb.toString());
	}

	public ResponseMessage createXtResMessage(ActionscriptObject asObj, List<SocketChannel> recipients) {
		try {
			return new XtResMessage(asObj, recipients);
		} catch (IOException e) {
			ServerLogger.error("createXtResMessage error : ", e);
			return null;
		}
	}
}