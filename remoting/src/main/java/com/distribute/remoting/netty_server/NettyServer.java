package com.distribute.remoting.netty_server;

import com.distribute.remoting.Message.Message;
import com.distribute.remoting.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
@Component
public class NettyServer {

    @Value("${remoting.address}")
    private String serverAddress;

    private Integer port= 8099;

    // 1.bossGroup 用于接收连接，workerGroup 用于具体的处理
    private final EventLoopGroup bossGroup= new NioEventLoopGroup(1);;
    private final EventLoopGroup workerGroup =new NioEventLoopGroup();

    private final EventLoopGroup sendGroup=new DefaultEventLoopGroup();

//    @PostConstruct
    public  void start() throws InterruptedException {
        try {
            //2.创建服务端启动引导/辅助类：ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            //3.给引导类配置两大线程组,确定了线程模型
            b.group(bossGroup, workerGroup)
                    // (非必备)打印日志
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ProcotolFrameDecoder());
                            p.addLast(new LoggingHandler(LogLevel.DEBUG));
                            p.addLast(new MessageCodecSharable());
                            p.addLast(new PingHandler());
                            p.addLast(new RegisterHandler());
                            p.addLast(new CallBackMessageHandler());
                            p.addLast(new ResponseHandler());
                            p.addLast(new ChatServerHandler());
                        }
                    });
            log.info("serverAddr:"+this.serverAddress);
            // 6.绑定端口,调用 sync 方法阻塞知道绑定完成
            ChannelFuture f = b.bind(this.serverAddress.split(":")[0], Integer.parseInt(this.serverAddress.split(":")[1])).sync();
            // 7.阻塞等待直到服务器Channel关闭(closeFuture()方法获取Channel 的CloseFuture对象,然后调用sync()方法)
            f.channel().closeFuture().sync();
            log.info("serverAddr:"+this.serverAddress+"closed!");
//            NioEventLoop eventExecutors = new NioEventLoop();
//            StringBuilder sb=new StringBuilder();
        } finally {
            //8.优雅关闭相关线程组资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    //3次重试，还失败就记录日志
    public void sendMessage(Message msg, Integer time,Channel channel){
        if(channel==null){
            return;
        }
        if(time<3){
            ChannelFuture channelFuture = channel.writeAndFlush(msg);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //写操作完成，并没有错误发生
                    if (future.isSuccess()){
                        log.info("send msg successful:"+msg+" id:"+msg.getRequestId());
                    }else{
                        //记录错误
                        log.info("send msg error! time:"+time+" msg:"+msg);
                        log.info("cause:"+future.cause().getMessage());
                        future.cause().printStackTrace();
                        sendMessage(msg,time+1,channel);
                    }
                }
            });
        }else{
            //todo 进行报警等其他操作
            log.info("send msg error >= 3 times CARE");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new NettyServer().start();

    }
}
