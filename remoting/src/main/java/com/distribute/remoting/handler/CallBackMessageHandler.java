package com.distribute.remoting.handler;

import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.RegisterInMessage;
import com.distribute.remoting.netty_server.nameServerController;
import com.distribute.remoting.netty_server.routeInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallBackMessageHandler  extends SimpleChannelInboundHandler<CallBackMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CallBackMessage msg) throws Exception {
        nameServerController instance =(nameServerController)Context.getBean("nameServerController");
        instance.handleCallBackMessage(msg);

    }
}
