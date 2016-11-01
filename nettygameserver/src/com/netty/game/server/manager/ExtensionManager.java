package com.netty.game.server.manager;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.netty.game.server.domain.Command;
import com.netty.game.server.handler.INettyExtension;
import com.netty.game.server.handler.ext.ClockExtension;
import com.netty.game.server.handler.ext.SmallGameExtension;

public class ExtensionManager {
	private static final Map<String, InvokeInfo> cmd_Invoke = new HashMap<>();

	static {
		add(new ClockExtension(100, "测试ext,接收前端命令并返回,定时回发消息到前端"));
		add(new SmallGameExtension(101, "小游戏"));
	}
	
	public static final ExtensionManager instance = new ExtensionManager();
	
	private ExtensionManager() {
		
	}

	public void init(){
		
	}
	
	public InvokeInfo getInvokeInfo(String cmd){
		return cmd_Invoke.get(cmd);
	}
	
	private static void add(INettyExtension extension) {
		String curXtName = extension.getClass().getSimpleName();
		Method[] methods = extension.getClass().getDeclaredMethods();
		for (Method method : methods) {
			Command cmd = method.getAnnotation(Command.class);
			if (cmd == null) {
				continue;
			}
			InvokeInfo invokeInfo = cmd_Invoke.get(cmd.value());
			if(invokeInfo != null){
				System.err.println(String.format("[%s] and [%s] have same cmd[%s].", curXtName, invokeInfo.instance.getClass().getSimpleName(), cmd.value()));
				throw new RuntimeException(String.format("[%s] and [%s] have same cmd[%s].", curXtName, invokeInfo.instance.getClass().getSimpleName(), cmd.value()));
			}
			cmd_Invoke.put(cmd.value(), new InvokeInfo(extension, method));
		}
	}
	
	public static class InvokeInfo{
		public final Object instance;
		public final Method method;
		
		public InvokeInfo(Object instance, Method method) {
			super();
			this.instance = instance;
			this.method = method;
		}
	}
}