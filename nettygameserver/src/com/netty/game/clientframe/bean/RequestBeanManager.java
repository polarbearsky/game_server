package com.netty.game.clientframe.bean;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.util.PackageFileScan;

public class RequestBeanManager {
	private static final Map<String, Constructor<?>> cmd_Construct = new HashMap<>();
	
	public static final RequestBeanManager instance = new RequestBeanManager();
	
	RequestBeanManager(){
		_init();
	}
	
	private void _init(){
		String package4Scan = "com.netty.game.jprotobuf.bean";
		Set<Class<?>> clazzSet = PackageFileScan.getClasses(package4Scan);
		for(Class<?> clazz : clazzSet){
			MsgAnnotation annotation = clazz.getAnnotation(MsgAnnotation.class);
			if(annotation == null || annotation.type() != RequestResponse.REQUEST){
				return;
			}
			try {
				cmd_Construct.put(annotation.cmd(), clazz.getConstructor());
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void init(){
		
	}
	
	/**
	 * 暂时只支持基本类型赋值
	 */
	public Object convert2Object(String cmd, String jsonParams){
		/*Constructor<?> construct = cmd_Construct.get(cmd);
		if(construct == null){
			return null;
		}
		Object object = construct.newInstance();
		String[] file2ValueArray = params.split(";");
		
		Parameter[] parameters = construct.getParameters();
		for(Parameter paramter : parameters){
			paramter.
		}*/
		return null;
	}
}
