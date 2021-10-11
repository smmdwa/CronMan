package com.distribute.executor.handler;

import com.distribute.executor.netty_client.execController;
import com.distribute.executor.utils.Context;
import com.distribute.remoting.Message.KillJobMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.Message.SendJobMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KillJobMessageHandler  extends SimpleChannelInboundHandler<KillJobMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, KillJobMessage msg) throws Exception {
        log.info("msg:"+msg);
        execController controller = (execController) Context.getBean(execController.class);
        ResponseMessage response = controller.killOldJob(msg);

        controller.sendResponse(response);

    }
}
