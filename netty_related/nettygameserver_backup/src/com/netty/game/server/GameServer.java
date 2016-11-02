package com.netty.game.server;

import com.netty.game.jprotobuf.JProtobufBeanManager;
import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.handler.LoginHandler;
import com.netty.game.server.msg.ServerCustomMsg;
import com.netty.game.server.threadpool.OutBoundTheadPoolService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class GameServer {

	public void bind(int port) throws Exception {
		JProtobufBeanManager.instance.init();
		
		// 配置服务端的NIO线程组
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
					.handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) {
							ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
							ch.pipeline().addLast(new ProtobufDecoder(ServerCustomMsg.CustomMsg.getDefaultInstance()));
							ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
							ch.pipeline().addLast(new ProtobufEncoder());
							ch.pipeline().addLast(ServerConfigData.LOGIN_HANDLER_NAME, new LoginHandler());
						}
					});
			
			
			
			OutBoundTheadPoolService.instance.init();
			
			// 打印版本
			printGameVersion();
			
			// 绑定端口，同步等待成功
			ChannelFuture f = b.bind(port).sync();

			// 等待服务端监听端口关闭
			f.channel().closeFuture().sync();
		} finally {
			// 优雅退出，释放线程池资源
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	private static void printGameVersion(){
		String versionFormat = "*********************************************\n"
							+"*******SERVER_VERSION : %s*******\n"
							+"*********************************************";
		System.out.println(String.format(versionFormat, GameVersion.GAME_VERSION));
	}
	
	public static void main(String[] args) throws Exception {
		int port = ServerConfigData.DEFAULT_SERVER_PORT;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}
		}
		new GameServer().bind(port);
	}
}
