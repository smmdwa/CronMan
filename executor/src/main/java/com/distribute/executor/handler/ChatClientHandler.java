package com.distribute.executor.handler;



import com.distribute.executor.netty_client.execController;
import com.distribute.executor.utils.Context;
import com.distribute.remoting.Message.PingMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        System.out.println("last:"+msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;
        // 触发了写空闲事件
        if (event.state() == IdleState.WRITER_IDLE) {
            PingMessage pingMessage = new PingMessage(name);
            log.info("发送心跳包："+pingMessage);
            ctx.writeAndFlush(pingMessage);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("server lose connection ==========");
        //中断所有的任务
        execController controller = (execController) Context.getBean(execController.class);
        controller.killAllJob();
    }
}
