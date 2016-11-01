package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.lib.ActionscriptObject;

public class XtReqMessage extends RequestMessage {
	public int extenionId;
	public String cmd;
	public ActionscriptObject paramObj = null;

	XtReqMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		this.extenionId = this.getShort();
		this.cmd = this.getString();
		this.isValid = ActionscriptObject.exMessageCodec.setXtReqMessageAsObj(this, this.buffer);
	}
}