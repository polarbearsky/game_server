package com.altratek.altraserver.lib;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.request.XtReqMessage;

public interface IExMessageCodec
{
	boolean setXtReqMessageAsObj(XtReqMessage msg, IoBuffer buffer);
	ResponseMessage createXtResMessage(ActionscriptObject asObj, List<SocketChannel> recipients);
}