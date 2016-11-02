package com.netty.game.server.threadpool;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.netty.game.server.domain.GameUser;
import com.netty.game.server.manager.ChannelManager;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.CustomParam;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.ParamValue;

public class OutBoundTheadPoolService {
	public static final OutBoundTheadPoolService instance = new OutBoundTheadPoolService();
	
	private ScheduledExecutorService scheduler;
	
	private OutBoundTheadPoolService(){
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new OutMsgJob(), 10, 5, TimeUnit.SECONDS);
	}
	
	public void init(){
		
	}
	
	
	private class OutMsgJob implements Runnable{
		
		@Override
		public void run() {
			for(GameUser user : ChannelManager.instance.getAllUsers()){
				if(user.getChannel().isActive()){
					user.getChannel().writeAndFlush(resp());
				}
			}
		}
		
		private ServerCustomMsg.CustomMsg resp(){	
			ParamValue.Builder timeParam = ParamValue.newBuilder();
			timeParam.addValue(String.valueOf(new Date()));	
			CustomParam.Builder timeCustomParam = CustomParam.newBuilder();
			timeCustomParam.setParamKey("time");
			timeCustomParam.setParamValues(timeParam);
			
			CustomMsg.Builder msgBuild = CustomMsg.newBuilder();
			msgBuild.setCmd(0);
			msgBuild.addParams(timeCustomParam);
			
			return msgBuild.build();
		}
	}
	
}
