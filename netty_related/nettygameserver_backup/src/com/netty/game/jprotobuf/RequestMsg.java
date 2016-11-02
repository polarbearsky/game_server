package com.netty.game.jprotobuf;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

public class RequestMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
    private int type;

    @Protobuf(fieldType = FieldType.INT32, order = 2, required = true)
    private int action;
    
    @Protobuf(fieldType = FieldType.STRING, order = 3, required = true)
    private String cmd;
    
    @Protobuf(fieldType = FieldType.BYTES, order = 4, required = true)
    private byte[] bytes;

    private static final Codec<RequestMsg> codec = ProtobufProxy.create(RequestMsg.class);
    
    
    
	public void setType(int type) {
		this.type = type;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public int getType() {
		return type;
	}

	public int getAction() {
		return action;
	}

	public String getCmd() {
		return cmd;
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	
}
