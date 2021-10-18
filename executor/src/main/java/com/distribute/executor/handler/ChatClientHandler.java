package com.distribute.executor.handler;



import com.distribute.executor.Message.ResponseMessage;
import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.netty_client.execController;
import com.distribute.executor.response.defaultFuture;
import com.distribute.executor.utils.Context;
import com.distribute.executor.Message.PingMessage;
import com.distribute.executor.utils.FutureUtil;
import com.distribute.executor.utils.idUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@AllArgsConstructor
@Slf4j
public class ChatClientHandler extends SimpleChannelInboundHandler<String> {
    private String message;
    private String name;
    private String addr;
    public String getMessage() {
        return message;
    }
    public ChatClientHandler(String name,String addr){
        this.name=name;
        this.addr=addr;
    }


    //读取服务器广播的消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//        message=msg.trim();
//        System.out.println("last:"+msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;
        // 触发了写空闲事件
        if (event.state() == IdleState.WRITER_IDLE) {
            NettyClient instance =(NettyClient) Context.getBean("nettyClient");
            // big bug 這裏也需要新开一个线程，不然会堵住handler
            instance.getSendExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    ReadySendPing(instance,ctx.channel());
                }
            });
        }
    }

    private void ReadySendPing( NettyClient instance,Channel channel){
        int time=0;
        while (time<3){
            if(SendPing(instance,channel)){
                break;
            }else {
                time++;
            }
        }
        //重试三次还失败，证明断线了，需要剔除出列表
        if(time>=3){
            log.info("lose connection ping");
            instance.removeScheduler(channel);
        }
    }

    private boolean SendPing(NettyClient instance,Channel channel){
        //创建requestId和futureMap
        PingMessage msg = new PingMessage(name,new idUtil().nextId());
        defaultFuture future = new defaultFuture();
        Map<Long, defaultFuture> futureMap = instance.getFutureMap();
        futureMap.put(msg.getRequestId(), future);

        //发送ping
        instance.sendMessage(channel,msg,0);

        //等待响应
        ResponseMessage responseMessage = FutureUtil.getFuture(futureMap, msg.getRequestId());
        if (responseMessage == null || responseMessage.getCode() == ResponseMessage.error) {
            log.info("ping fail");
            return false;
        }else{
            return true;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyClient instance =(NettyClient) Context.getBean("nettyClient");
        Channel channel=ctx.channel();
        //删除ctx对应的服务器
        instance.removeScheduler(channel);
        log.info("server{} lose connection ==========",ctx.channel());
        //中断所有的任务
        execController controller = (execController) Context.getBean(execController.class);
        controller.killAllJob();
    }
}
