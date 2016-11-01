package com.netty.game.jprotobuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.util.PackageFileScan;


public class JProtobufBeanManager {
	private static final Map<RequestResponse, Map<String, Codec<?>>> type_key_Codec = new HashMap<>();
	
	public static final JProtobufBeanManager instance = new JProtobufBeanManager();
	
	JProtobufBeanManager(){
		_init();
	}
	
	private void _init(){
		String package4Scan = "com.netty.game.jprotobuf.bean";
		type_key_Codec.clear();
		Set<Class<?>> classSet = PackageFileScan.getClasses(package4Scan);
		for(Class<?> clazz : classSet){
			MsgAnnotation annotation = clazz.getAnnotation(MsgAnnotation.class);
			if(annotation == null){
				return;
			}
			RequestResponse type = annotation.type();
			if(type == null){
				System.err.println(String.format("[%s] annotation type is null!", clazz.getName()));
			}
			Map<String, Codec<?>> key_Codec = type_key_Codec.get(type);
			if(key_Codec == null){
				key_Codec = new HashMap<>();
				type_key_Codec.put(type, key_Codec);
			}
			String cmd = annotation.cmd();
			if(key_Codec.containsKey(cmd)){
				System.err.println(String.format("duplicate codec key[%s]!", cmd));
			}
			Codec<?> codec = ProtobufProxy.create(clazz);
			key_Codec.put(cmd, codec);
		}
	}
	
	public void init(){
		
	}
	
	public Codec<?> getCodec(RequestResponse type, String cmd){
		Map<String, Codec<?>> key_Codec = type_key_Codec.get(type);
		if(key_Codec == null){
			return null;
		}
		return key_Codec.get(cmd);
	}

}
