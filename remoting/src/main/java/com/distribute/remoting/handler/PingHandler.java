package com.distribute.remoting.handler;

import com.distribute.remoting.Message.Message;
import com.distribute.remoting.Message.PingMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.netty_server.NameServerController;
import com.distribute.remoting.netty_server.RouteInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingHandler extends SimpleChannelInboundHandler<PingMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, PingMessage pingMessage) throws Exception {
        String name = pingMessage.getName();
        RouteInfoManager manager = (RouteInfoManager) Context.getBean(RouteInfoManager.class);
        NameServerController controller =(NameServerController)Context.getBean("nameServerController");
        boolean result = manager.updateActiveExecutor(name);
        if(result){
            //返回response消息
            ResponseMessage responseMessage = new ResponseMessage(pingMessage.getRequestId(), Message.success, "pinged");
            controller.sendResponse(responseMessage, channelHandlerContext.channel());
        }
    }
}
