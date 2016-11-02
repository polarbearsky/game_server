/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.netty.game.server.handler;

import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.domain.GameUser;
import com.netty.game.server.manager.ChannelManager;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.CustomParam;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.ParamValue;
import com.netty.game.server.util.StringUtils;
import com.netty.game.server.validate.AccountValidate;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class LoginHandler extends ChannelHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {		
		ServerCustomMsg.CustomMsg req = (ServerCustomMsg.CustomMsg) msg;
		System.out.println("received msg: \n" + req);
		
		if(req.getCmd() != ServerConfigData.CMD_LOGIN){
			ctx.writeAndFlush(resp(ServerConfigData.CMD_LOGIN, -1, "登陆命令错误"));
			ctx.close();
			throw new IllegalArgumentException("<LoginHandler>cmd error! must be login cmd!");
		}
		String userName = req.getParamValueByKey(ServerConfigData.KEY_PARAM_USER_NAME);
		String pwd = req.getParamValueByKey(ServerConfigData.KEY_PARAM_PASSWORD);
		if(StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)){
			ctx.writeAndFlush(resp(ServerConfigData.CMD_LOGIN, -2, "登陆参数错误"));
			ctx.close();
			throw new IllegalArgumentException("<LoginHandler>param empty!");
		}
		if(!AccountValidate.INSTANCE.isValidate(userName, pwd)){
			ctx.writeAndFlush(resp(ServerConfigData.CMD_LOGIN, -3, "登陆账号密码错误"));
			ctx.close();
			throw new IllegalArgumentException(String.format("<LoginHandler>account validate error! user is %s", userName));
		}
		ctx.pipeline().replace(ServerConfigData.LOGIN_HANDLER_NAME, ServerConfigData.EXT_HANDLER_NAME, new ExtHandler());
		ctx.writeAndFlush(resp(ServerConfigData.CMD_LOGIN, 1, "登陆成功"));
		GameUser user = new GameUser(userName, ctx.channel());
		ChannelManager.instance.addUser(user);
	}

	private ServerCustomMsg.CustomMsg resp(int cmd, int result, String desc){	
		ParamValue.Builder resultParam = ParamValue.newBuilder();
		resultParam.addValue(String.valueOf(result));	
		CustomParam.Builder resultCustomParam = CustomParam.newBuilder();
		resultCustomParam.setParamKey("r");
		resultCustomParam.setParamValues(resultParam);
		
		ParamValue.Builder descParam = ParamValue.newBuilder();
		descParam.addValue(desc);
		CustomParam.Builder descCustomParam = CustomParam.newBuilder();
		descCustomParam.setParamKey("desc");
		descCustomParam.setParamValues(descParam);
		
		CustomMsg.Builder msgBuild = CustomMsg.newBuilder();
		msgBuild.setCmd(cmd);
		msgBuild.addParams(resultCustomParam);
		msgBuild.addParams(descCustomParam);
		
		return msgBuild.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		System.err.println("<LoginHandler catch case>");
		ctx.close();
	}
}
