package com.distribute.remoting.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class PongHandler extends ChannelInboundHandlerAdapter{
    // 接收响应消息
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("pong:"+msg);
    }
}
