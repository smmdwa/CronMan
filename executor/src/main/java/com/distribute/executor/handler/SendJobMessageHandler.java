package com.distribute.executor.handler;


import com.distribute.executor.netty_client.execController;
import com.distribute.executor.Message.SendJobMessage;
import com.distribute.executor.utils.Context;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

//给client的handler  用于执行job
@Slf4j
public class SendJobMessageHandler extends SimpleChannelInboundHandler<SendJobMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, SendJobMessage sendJobMessage) throws Exception {
        log.info("msg:"+sendJobMessage);
        execController controller = (execController)Context.getBean(execController.class);
        controller.addNewJob(sendJobMessage);
    }
}
