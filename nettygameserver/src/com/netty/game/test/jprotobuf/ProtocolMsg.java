package com.netty.game.test.jprotobuf;

import java.util.Map;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

public class ProtocolMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int cmd;
	
	@Protobuf(fieldType = FieldType.MAP, order = 2, required = true)
	private Map<String, Object> key2Value;
}
