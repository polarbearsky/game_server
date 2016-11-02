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
package com.netty.game.client.handler;

import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.CustomParam;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.ParamValue;
import com.netty.game.server.util.StringUtils;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class ClientExtHandler extends ChannelHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(buildExtReq(ServerConfigData.CMD_EXT, "1_1_1", "dev"));
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ServerCustomMsg.CustomMsg resp = (CustomMsg) msg;
		System.out.println("received msg: \n" + resp);
	}

	private ServerCustomMsg.CustomMsg buildExtReq(int cmd, String subCmd, String mySubCmdParam) {
		ParamValue.Builder subCmdParam = ParamValue.newBuilder();
		subCmdParam.addValue(subCmd);
		CustomParam.Builder subCmdCustomParam = CustomParam.newBuilder();
		subCmdCustomParam.setParamKey(ServerConfigData.KEY_SUB_CMD);
		subCmdCustomParam.setParamValues(subCmdParam);

		CustomMsg.Builder msgBuild = CustomMsg.newBuilder();
		msgBuild.setCmd(cmd);
		msgBuild.addParams(subCmdCustomParam);
		if (!StringUtils.isBlank(mySubCmdParam)) {
			ParamValue.Builder _mySubCmdParam = ParamValue.newBuilder();
			_mySubCmdParam.addValue(mySubCmdParam);
			CustomParam.Builder mySubCmdCustomParam = CustomParam.newBuilder();
			mySubCmdCustomParam.setParamKey(ServerConfigData.KEY_SUBCMD_PARAM);
			mySubCmdCustomParam.setParamValues(_mySubCmdParam);
			msgBuild.addParams(mySubCmdCustomParam);
		}

		return msgBuild.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
