package com.distribute.remoting.handler;

import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.netty_server.nameServerController;
import com.distribute.remoting.utils.Context;
import com.distribute.remoting.utils.FutureUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ResponseHandler extends SimpleChannelInboundHandler<ResponseMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ResponseMessage msg) throws Exception {
        nameServerController instance =(nameServerController) Context.getBean("nameServerController");
        if( !FutureUtil.setFuture(instance.getFutureMap(),msg)){
            log.info("no such requestId");
        }

    }
}

