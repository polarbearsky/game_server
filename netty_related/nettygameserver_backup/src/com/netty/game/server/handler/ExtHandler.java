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
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.CustomParam;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.ParamValue;
import com.netty.game.server.util.StringUtils;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class ExtHandler extends ChannelHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ctx.channel().pipeline().writeAndFlush(msg);
		ServerCustomMsg.CustomMsg req = (ServerCustomMsg.CustomMsg) msg;
		System.out.println("received msg: \n" + req);
		if (req.getCmd() != ServerConfigData.CMD_EXT) {
			ctx.writeAndFlush(resp(ServerConfigData.CMD_EXT, "subCmd", null, -1, "扩展命令错误"));
			ctx.close();
			throw new IllegalArgumentException("<ExtHandler>cmd error! must be ext cmd!");
		}
		String subCmd = req.getParamValueByKey(ServerConfigData.KEY_SUB_CMD);
		String subCmdParam = req.getParamValueByKey(ServerConfigData.KEY_SUBCMD_PARAM);
		if (StringUtils.isBlank(subCmd)) {
			ctx.writeAndFlush(resp(ServerConfigData.CMD_EXT, "subCmd", null, -2, "扩展子命令为空"));
			ctx.close();
			throw new IllegalArgumentException("<ExtHandler>subCmd param empty!");
		}
		ctx.writeAndFlush(resp(ServerConfigData.CMD_EXT, subCmd, subCmdParam, 1, "ext处理成功"));
	}

	private ServerCustomMsg.CustomMsg resp(int cmd, String subCmd, String mySubCmdParam, int result, String desc) {
		ParamValue.Builder subCmdParam = ParamValue.newBuilder();
		subCmdParam.addValue(subCmd);
		CustomParam.Builder subCmdCustomParam = CustomParam.newBuilder();
		subCmdCustomParam.setParamKey(ServerConfigData.KEY_SUB_CMD);
		subCmdCustomParam.setParamValues(subCmdParam);

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
		msgBuild.addParams(subCmdCustomParam);
		if (!StringUtils.isBlank(mySubCmdParam)) {
			ParamValue.Builder _mySubCmdParam = ParamValue.newBuilder();
			subCmdParam.addValue(mySubCmdParam);
			CustomParam.Builder mySubCmdCustomParam = CustomParam.newBuilder();
			mySubCmdCustomParam.setParamKey(ServerConfigData.KEY_SUBCMD_PARAM);
			mySubCmdCustomParam.setParamValues(_mySubCmdParam);
			msgBuild.addParams(mySubCmdCustomParam);
		}
		msgBuild.addParams(resultCustomParam);
		msgBuild.addParams(descCustomParam);

		return msgBuild.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		System.err.println("<ExtHandler catch case>");
		ctx.close();
	}
}
