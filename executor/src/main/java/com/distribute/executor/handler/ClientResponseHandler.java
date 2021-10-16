package com.distribute.executor.handler;

import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.netty_client.execController;
import com.distribute.executor.utils.Context;
import com.distribute.executor.utils.FutureUtil;
import com.distribute.executor.Message.ResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientResponseHandler extends SimpleChannelInboundHandler<ResponseMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ResponseMessage msg) throws Exception {
        NettyClient instance =(NettyClient) Context.getBean("nettyClient");
        if( !FutureUtil.setFuture(instance.getFutureMap(),msg)){
            log.info("no such requestId! msg:{},id:{}",msg,msg.getRequestId());
        }
    }
}

