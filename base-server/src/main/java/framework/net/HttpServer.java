package framework.net;

import framework.BaseServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Created by Administrator on 2018/5/3.
 */
public class HttpServer {

	private BaseServer m_server;

	public HttpServer(BaseServer baseServer) {
		this.m_server = baseServer;
	}

	public void start(final int port) throws Exception {
		EventLoopGroup boss = new NioEventLoopGroup();
		EventLoopGroup woker = new NioEventLoopGroup();
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		try {
			serverBootstrap.channel(NioServerSocketChannel.class).group(boss, woker)
					.childOption(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.SO_BACKLOG, 1024)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast("http-decoder", new HttpServerCodec())
									.addLast("aggregator", new HttpObjectAggregator(1024 * 1024)) // 在处理
																									// POST消息体时需要加上
									.addLast(new HttpServerHandler(m_server));
						}
					});
			serverBootstrap.bind(port).sync();
			//ChannelFuture future = serverBootstrap.bind(port).sync();
			// future.channel().closeFuture().sync();
		} finally {
			// boss.shutdownGracefully();
			// woker.shutdownGracefully();
		}
	}
}
