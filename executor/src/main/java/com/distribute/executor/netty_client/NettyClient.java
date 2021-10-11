package com.distribute.executor.netty_client;

import com.distribute.executor.handler.ChatClientHandler;
import com.distribute.executor.handler.KillJobMessageHandler;
import com.distribute.executor.handler.SendJobMessageHandler;
import com.distribute.remoting.Message.Message;
import com.distribute.remoting.Message.RegisterInMessage;
import com.distribute.remoting.handler.MessageCodecSharable;
import com.distribute.remoting.handler.ProcotolFrameDecoder;
import com.distribute.remoting.handler.ResponseHandler;
import com.distribute.remoting.response.defaultFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@Data
//@ConfigurationProperties(prefix = "executor")
public class NettyClient {
//    @Value("${executor.addr}")
    private String addr;

    @Value("${executor.ip}")
    private String ip;

    @Value("${executor.port}")
    private Integer port;

    @Value("${executor.name}")
    private String name;

    private Channel channel;

    private final Map<Long, defaultFuture> futureMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void initialize(){

        this.addr=this.ip+":"+this.port;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        log.info("initialize ===== ok");
    }
    public void start() throws InterruptedException {
        //1.创建一个 NioEventLoopGroup 对象实例
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //2.创建客户端启动引导/辅助类：Bootstrap
            Bootstrap b = new Bootstrap();
            //3.指定线程组
            b.group(group)
                    //4.指定 IO 模型
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ProcotolFrameDecoder());
//                            p.addLast(new LoggingHandler(LogLevel.DEBUG));
                            p.addLast(new MessageCodecSharable());
                            p.addLast(new IdleStateHandler(0, 3, 0));
                            p.addLast(new SendJobMessageHandler());
                            p.addLast(new KillJobMessageHandler());
                            p.addLast(new ResponseHandler());
                            p.addLast(new ChatClientHandler( name, addr));
                        }
                    });
            // 6.尝试建立连接
//            ChannelFuture f = b.connect(ip,port).sync();
            ChannelFuture f = b.connect("localhost",8099).sync();

            f.sync();
            this.channel= f.channel();

            // 发送注册请求
            sendMessage(new RegisterInMessage(addr,name,5),0);

            log.info("start ===== ok");

            // 7.等待连接关闭（阻塞，直到Channel关闭）
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    //3次重试，还失败就记录日志
    public void sendMessage(Message msg,Integer time){
        if(time<3){
            ChannelFuture channelFuture = this.channel.writeAndFlush(msg);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //写操作完成，并没有错误发生
                    if (future.isSuccess()){
                        log.info("send msg successful:"+msg);
                    }else{
                        //记录错误
                        log.info("send msg error! time:"+time+" msg:"+msg);
                        future.cause().printStackTrace();
                        sendMessage(msg,time+1);
                    }
                }
            });
        }else{
            //todo 进行报警等其他操作
            log.info("send msg error >= 3 times CARE");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new NettyClient().start();
    }
}
