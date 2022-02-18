package com.distribute.remoting.handler;

import com.distribute.remoting.netty_server.RouteInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
@Slf4j
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    //通道集合，一个通道就是一个客户端，将所有客户端放在一个集合中
    private static List<Channel> channels=new ArrayList<Channel>();

    //通道就绪，就是客户端连接上了
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel inChannel=ctx.channel();
        //在线就把该通道（客户端）添加到集合中
        channels.add(inChannel);
        log.info("[服务器端]："+inChannel.remoteAddress().toString().substring(1)+"上线");

    }
    //通道未就绪，就是客户端未连接
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel inChannel=ctx.channel();
        //离线就把该客户端踢出集合
        channels.remove(inChannel);
        RouteInfoManager manager = (RouteInfoManager) Context.getBean(RouteInfoManager.class);

        String name = manager.getExecutorName(inChannel);
        if(name!=null){
            inChannel.close();
            manager.destory(name);
            manager.changeCode(name);
        }
        log.info("[服务器端]："+inChannel.remoteAddress().toString().substring(1)+"离线");
    }
    //读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel inChannel=ctx.channel();
        //读取的数据广播出去，但不能给当前通道
        for(Channel channel:channels) {
            if(channel!=inChannel) {
                channel.writeAndFlush("[客户端："+inChannel.remoteAddress().toString().substring(1)+"]"+"："+msg+"\n");
            }
        }
    }
}
