package com.distribute.remoting.handler;

import com.distribute.remoting.Message.PingMessage;
import com.distribute.remoting.netty_server.routeInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PingHandler extends SimpleChannelInboundHandler<PingMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, PingMessage pingMessage) throws Exception {
        String name = pingMessage.getName();
        routeInfoManager manager = (routeInfoManager) Context.getBean(routeInfoManager.class);
        boolean result = manager.updateActiveExecutor(name);
        if(!result){
            System.out.println("PingHandler === 出错了！！");
        }
    }
}
