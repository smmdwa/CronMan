package com.distribute.remoting.handler;

import com.distribute.remoting.Message.Message;
import com.distribute.remoting.Message.PingMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.netty_server.nameServerController;
import com.distribute.remoting.netty_server.routeInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingHandler extends SimpleChannelInboundHandler<PingMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, PingMessage pingMessage) throws Exception {
        String name = pingMessage.getName();
        routeInfoManager manager = (routeInfoManager) Context.getBean(routeInfoManager.class);
        nameServerController controller =(nameServerController)Context.getBean("nameServerController");
        boolean result = manager.updateActiveExecutor(name);
        if(!result){
            log.info("RegisterHandler === 出错 msg:{}",pingMessage);
        }else{
            //返回response消息
            ResponseMessage responseMessage = new ResponseMessage(pingMessage.getRequestId(), Message.success, "pinged");
            controller.sendResponse(responseMessage, channelHandlerContext.channel());
        }
    }
}
