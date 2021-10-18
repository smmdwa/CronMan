package com.distribute.remoting.handler;


import com.distribute.remoting.Message.RegisterInMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.netty_server.NameServerController;
import com.distribute.remoting.netty_server.RouteInfoManager;
import com.distribute.remoting.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterHandler extends SimpleChannelInboundHandler<RegisterInMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RegisterInMessage msg) throws Exception {
        String name = msg.getName();
        String addr = msg.getAddr();
        Integer level=msg.getLevel();

        log.info("register name:"+name+" addr:"+addr+" level:"+level+" channel:"+channelHandlerContext.channel());
        RouteInfoManager manager = (RouteInfoManager) Context.getBean(RouteInfoManager.class);
        NameServerController controller =(NameServerController)Context.getBean("nameServerController");

        boolean result = manager.registerExecutor(name, addr, channelHandlerContext.channel(),level);

        if(!result){
            log.info("RegisterHandler === 出错 msg:{}",msg);
        }else{
            //返回response消息
            ResponseMessage responseMessage = new ResponseMessage(msg.getRequestId(), ResponseMessage.success, "registered");
            controller.sendResponse(responseMessage, channelHandlerContext.channel());
        }
    }
}
