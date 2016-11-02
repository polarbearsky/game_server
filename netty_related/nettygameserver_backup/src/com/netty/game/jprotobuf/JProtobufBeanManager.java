package com.netty.game.jprotobuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.netty.game.test.jprotobuf.MsgAnnotation;
import com.netty.game.test.jprotobuf.PackageFileScan;

public class JProtobufBeanManager {
	private static final Map<String, Codec<?>> key2Codec = new HashMap<>();
	
	public static final JProtobufBeanManager instance = new JProtobufBeanManager();
	
	JProtobufBeanManager(){
		_init();
	}
	
	private void _init(){
		String package4Scan = "com.netty.game.jprotobuf.bean";
		key2Codec.clear();
		Set<Class<?>> classSet = PackageFileScan.getClasses(package4Scan);
		for(Class<?> clazz : classSet){
			MsgAnnotation annotation = clazz.getAnnotation(MsgAnnotation.class);
			if(annotation == null){
				return;
			}
			if(key2Codec.containsKey(annotation.value())){
				System.err.println(String.format("duplicate codec key[%s]!", annotation.value()));
			}
			Codec<?> codec = ProtobufProxy.create(clazz);
			key2Codec.put(annotation.value(), codec);
		}
	}
	
	public void init(){
		
	}
	
	public Codec<?> getCodec(String cmd){
		return key2Codec.get(cmd);
	}
}
