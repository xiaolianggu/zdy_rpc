package com.lagou.handler;

import java.lang.reflect.Method;

import com.lagou.config.SpringContextUtil;
import com.lagou.serialize.RpcRequest;
import com.lagou.service.UserServiceImpl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class UserServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 判断是否符合约定，符合则调用本地方法，返回数据
        // msg:  UserService#sayHello#are you ok?
        if(msg.toString().startsWith("UserService")){
            UserServiceImpl userService = new UserServiceImpl();
            String result = userService.sayHello(msg.toString().substring(msg.toString().lastIndexOf("#") + 1));
            ctx.writeAndFlush(result);
        }
        
        
        RpcRequest request = (RpcRequest)msg;
        Class<?> clazz = Class.forName(request.getClassName());
        Object obj = SpringContextUtil.getBean(clazz);
        //Object obj =  clazz.newInstance();
        Method method = clazz.getMethod(request.getMethodName(),request.getParameterTypes());//得到方法对象
        Object result = method.invoke(obj, request.getParameters());
        ctx.writeAndFlush(result);
    }
}
