package com.netty.game.test.jprotobuf.bean;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.test.jprotobuf.MsgAnnotation;
import com.netty.game.test.jprotobuf.UniqueConfig;

@MsgAnnotation(UniqueConfig.Cmd_1_0_1)
public class ReponseMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int result;
	
	@Protobuf(fieldType = FieldType.STRING, order = 2, required = true)
	private String desc;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
