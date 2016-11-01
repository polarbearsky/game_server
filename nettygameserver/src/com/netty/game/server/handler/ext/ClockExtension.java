package com.netty.game.server.handler.ext;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.netty.game.jprotobuf.bean.ext.clock.HeartBeatRequestMsg;
import com.netty.game.jprotobuf.bean.ext.clock.HeartBeatResponseMsg;
import com.netty.game.jprotobuf.bean.ext.clock.TimerResponseMsg;
import com.netty.game.server.ServerLogger;
import com.netty.game.server.domain.Command;
import com.netty.game.server.domain.GameUser;
import com.netty.game.server.handler.INettyExtension;
import com.netty.game.server.manager.ChannelManager;
import com.netty.game.server.msg.MsgHelper;
import com.netty.game.server.schedulers.CommonJobContext;
import com.netty.game.server.schedulers.JobContext;
import com.netty.game.server.schedulers.ScheduleManagement;
import com.netty.game.server.util.RandomUtil;

public class ClockExtension implements INettyExtension {
	public static final String CMD_TIME = "100_1_1";
	public static final String ON_CMD_TIMER_MSG = "100_1_2";
	
	
	
	public ClockExtension(int extensionId, String desc){
		JobContext context = new CommonJobContext(10, 5, TimeUnit.SECONDS);
		ScheduleManagement.getInstance().register(new TimerTask(), context);
	}
	
	class TimerTask implements Runnable{

		@Override
		public void run() {
			TimerResponseMsg responseMsg = new TimerResponseMsg(RandomUtil.avgRandom(1, 10000));
			for(GameUser user : ChannelManager.instance.getAllUsers()){
				MsgHelper.sendExtMsg(user, responseMsg);
			}
		}
		
	}
	
	@Override
	public void init() {

	}

	@Override
	public void destroy() {

	}

	@Command(CMD_TIME)
	public void handleTime(GameUser user, HeartBeatRequestMsg msg){
		ServerLogger.info("recive client time:" + (new Date(msg.getClientTime())).toString());
		HeartBeatResponseMsg response = new HeartBeatResponseMsg(System.currentTimeMillis());
		MsgHelper.sendExtMsg(user, response);
	}
	
	
}
