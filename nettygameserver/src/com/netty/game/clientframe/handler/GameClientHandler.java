package com.netty.game.clientframe.handler;

import com.netty.game.client.handler.ClientLoginHandler;
import com.netty.game.clientframe.bean.LoginData;
import com.netty.game.clientframe.view.OutPutPanel;
import com.netty.game.jprotobuf.JProtobufBeanManager;
import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.msg.ServerCustomMsg;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class GameClientHandler {
	public final LoginData loginData;
	private EventLoopGroup group;
	private ChannelFuture channelFuture;
	
	public GameClientHandler(LoginData loginData){
		this.loginData = loginData;
	}
	
	public void connect(){
		JProtobufBeanManager.instance.init();
		group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
						ch.pipeline().addLast(new ProtobufDecoder(ServerCustomMsg.CustomMsg.getDefaultInstance()));
						ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
						ch.pipeline().addLast(new ProtobufEncoder());
						ch.pipeline().addLast(ServerConfigData.LOGIN_HANDLER_NAME, new ClientLoginHandler(loginData.userName, loginData.pwd));
					}
				});

		// 发起异步连接操作
		try {
			channelFuture = b.connect(loginData.ip, loginData.port).sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
			OutPutPanel.getInstance().addMsg(e.toString());
		}
	}

	public void disConnect(){
		if(group != null && channelFuture != null){
			// 当代客户端链路关
			try {
				channelFuture.channel().closeFuture().sync();
			} catch (InterruptedException e) {
				e.printStackTrace();
				OutPutPanel.getInstance().addMsg(e.toString());
			}finally {
				// 优雅退出，释放NIO线程
				group.shutdownGracefully();
			}
		}
	}
}
