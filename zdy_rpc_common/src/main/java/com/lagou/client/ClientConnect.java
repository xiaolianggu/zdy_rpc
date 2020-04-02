package com.lagou.client;

import com.lagou.serialize.JSONSerializer;
import com.lagou.serialize.RpcEncoder;
import com.lagou.serialize.RpcRequest;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

public class ClientConnect {

	private UserClientHandler userClientHandler;

	private EventLoopGroup group;

	private String ip;

	private Integer port;

	public void initClient(String ip, Integer port) {

		this.ip = ip;
		this.port = port;
		group = new NioEventLoopGroup();
		userClientHandler = new UserClientHandler();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new StringDecoder());
						pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));
						pipeline.addLast(userClientHandler);
					}
				});

		try {
			bootstrap.connect(ip, port).sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void close() {
		if (group != null) {
			group.shutdownGracefully();
		}
	}

	public UserClientHandler getUserClientHandler() {
		return userClientHandler;
	}

	public String getIp() {
		return ip;
	}

	public Integer getPort() {
		return port;
	}

}
