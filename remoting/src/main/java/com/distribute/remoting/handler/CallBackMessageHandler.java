package com.distribute.remoting.handler;

import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.netty_server.NameServerController;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallBackMessageHandler  extends SimpleChannelInboundHandler<CallBackMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CallBackMessage msg) throws Exception {
        NameServerController instance =(NameServerController)Context.getBean("nameServerController");
        ResponseMessage responseMessage = instance.handleCallBack(msg);
        instance.sendResponse(responseMessage,msg.getName());
    }
}
