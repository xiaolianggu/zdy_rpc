package com.lagou.serialize;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class RpcDecoder extends MessageToMessageDecoder<ByteBuf> {

	 private Class<?> clazz;

    private Serializer serializer;

    public RpcDecoder(Class<?> clazz, Serializer serializer) {

        this.clazz = clazz;

        this.serializer = serializer;

    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    	in.markReaderIndex();                  //我们标记一下当前的readIndex的位置
        int dataLength = in.readInt();       // 读取传送过来的消息的长度。ByteBuf 的readInt()方法会让他的readIndex增加4
        if (dataLength < 0) { // 我们读到的消息体长度为0，这是不应该出现的情况，这里出现这情况，关闭连接。
            ctx.close();
        }

        if (in.readableBytes() < dataLength) { //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[dataLength];  //  嗯，这时候，我们读到的长度，满足我们的要求了，把传送过来的数据，取出来吧~~
        in.readBytes(body);  //
        Object o = serializer.deserialize(clazz, body);  //将byte数据转化为我们需要的对象。伪代码，用什么序列化，自行选择
        out.add(o);  
    }
}