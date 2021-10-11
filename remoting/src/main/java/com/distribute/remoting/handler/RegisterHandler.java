package com.distribute.remoting.handler;


import com.distribute.remoting.Message.RegisterInMessage;
import com.distribute.remoting.netty_server.routeInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterHandler extends SimpleChannelInboundHandler<RegisterInMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RegisterInMessage registerInMessage) throws Exception {
        String name = registerInMessage.getName();
        String addr = registerInMessage.getAddr();
        Integer level=registerInMessage.getLevel();

        log.info("register name:"+name+" addr:"+addr+" level:"+level+" channel:"+channelHandlerContext.channel());
        routeInfoManager manager = (routeInfoManager) Context.getBean(routeInfoManager.class);


        boolean result = manager.registerExecutor(name, addr, channelHandlerContext.channel(),level);

        if(!result){
            System.out.println("RegisterHandler === 出错了！！");
        }
    }
}
