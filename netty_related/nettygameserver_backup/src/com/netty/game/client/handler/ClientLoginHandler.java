package com.netty.game.client.handler;

import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.CustomParam;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg.ParamValue;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class ClientLoginHandler extends ChannelHandlerAdapter {

	/**
	 * Creates a client-side handler.
	 */
	public ClientLoginHandler() {

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(buildLoginRequest(ServerConfigData.CMD_LOGIN, "polarbear", "polarbear"));
	}
	
	private ServerCustomMsg.CustomMsg buildLoginRequest(int cmd, String userName, String pwd) {
		ParamValue.Builder userNameParam = ParamValue.newBuilder();
		userNameParam.addValue(userName);
		CustomParam.Builder userNameCustomParam = CustomParam.newBuilder();
		userNameCustomParam.setParamKey(ServerConfigData.KEY_PARAM_USER_NAME);
		userNameCustomParam.setParamValues(userNameParam);

		ParamValue.Builder pwdParam = ParamValue.newBuilder();
		pwdParam.addValue(pwd);
		CustomParam.Builder pwdCustomParam = CustomParam.newBuilder();
		pwdCustomParam.setParamKey(ServerConfigData.KEY_PARAM_PASSWORD);
		pwdCustomParam.setParamValues(pwdParam);

		CustomMsg.Builder msgBuild = CustomMsg.newBuilder();
		msgBuild.setCmd(cmd);
		msgBuild.addParams(userNameCustomParam);
		msgBuild.addParams(pwdCustomParam);

		return msgBuild.build();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ServerCustomMsg.CustomMsg response = (ServerCustomMsg.CustomMsg) msg;
		System.out.println("received msg: \n" + response);
		int cmd = response.getCmd();
		String result = response.getParamValueByKey("r");
		String desc = response.getParamValueByKey("desc");
		if (ServerConfigData.CMD_LOGIN != cmd || !"1".equals(result)) {
			throw new IllegalArgumentException(
					String.format("<ClientLoginHandler> response error!cmd is %d, and result is %s, desc is %s", cmd,
							result == null ? "null" : result, desc == null ? "null" : desc));
		}
		ChannelHandlerAdapter handler = new ClientExtHandler();
		ctx.pipeline().replace(ServerConfigData.LOGIN_HANDLER_NAME, ServerConfigData.EXT_HANDLER_NAME, handler);
		handler.channelActive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
