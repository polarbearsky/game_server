package com.netty.game.jprotobuf;

import java.io.IOException;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.google.protobuf.ByteString;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;

public class CodecHelper {
	public static ServerCustomMsg.CustomMsg encode(int type, int action, String cmd, Object param){
		ServerCustomMsg.CustomMsg.Builder build = ServerCustomMsg.CustomMsg.newBuilder();
		build.setType(type);
		build.setAction(action);
		build.setCmd(cmd);
		if(param == null){
			build.setParams(ByteString.copyFrom(new byte[]{}));
		}else {
			MsgAnnotation annotation = param.getClass().getAnnotation(MsgAnnotation.class);
			@SuppressWarnings("unchecked")
			Codec<Object> codec = (Codec<Object>) JProtobufBeanManager.instance.getCodec(annotation.type(), annotation.cmd());
			try {
				build.setParams(ByteString.copyFrom(codec.encode(param)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return build.build();
	}
	
	
	public static Object decode4Server(CustomMsg customMsg){
		return decode(RequestResponse.REQUEST, customMsg);
	}
	
	/*所有消息的设置都是从server角度出发,所以client的解析要反过来*/
	public static Object decode4Client(CustomMsg customMsg){
		return decode(RequestResponse.RESPONSE, customMsg);
	}
	
	public static Object decode(RequestResponse type, CustomMsg customMsg){
		byte[] bytes = customMsg.getParams().toByteArray();
		if(bytes == null || bytes.length == 0){
			return null;
		}
		@SuppressWarnings("unchecked")
		Codec<Object> codec = (Codec<Object>) JProtobufBeanManager.instance.getCodec(type, customMsg.getCmd());
		try {
			return codec.decode(bytes);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
