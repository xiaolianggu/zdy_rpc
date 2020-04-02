package com.lagou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.lagou.handler.UserServerHandler;
import com.lagou.jdbc.JDBCHelper;
import com.lagou.serialize.JSONSerializer;
import com.lagou.serialize.RpcDecoder;
import com.lagou.serialize.RpcRequest;
import com.lagou.service.UserService;
import com.lagou.zk.ZooKeeperSession;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

@SpringBootApplication
public class RpcProviderApplication {


    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(RpcProviderApplication.class, args);
        startServer("127.0.0.1",8088);
        System.out.println(UserService.class.getName());
    }

    
    //hostName:ip地址  port:端口号
    public static void startServer(String hostName,int port) throws InterruptedException {

        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()));
                        pipeline.addLast(new UserServerHandler());

                    }
                });
        serverBootstrap.bind(hostName,port).sync();
        ZooKeeperSession.getInstance().init();
        ZooKeeperSession.getInstance().register(UserService.class.getName(), hostName+":"+port);
        JDBCHelper.getInstance();
       System.out.println("启动成功");
    }
}
