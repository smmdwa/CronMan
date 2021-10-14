package com.distribute.executor.netty_client;

import com.distribute.executor.handler.ChatClientHandler;
import com.distribute.executor.handler.ClientResponseHandler;
import com.distribute.executor.handler.KillJobMessageHandler;
import com.distribute.executor.handler.SendJobMessageHandler;
import com.distribute.executor.utils.DataUtil;
import com.distribute.executor.Message.Message;
import com.distribute.executor.Message.RegisterInMessage;
import com.distribute.executor.handler.MessageCodecSharable;
import com.distribute.executor.handler.ProcotolFrameDecoder;
import com.distribute.executor.handler.ResponseHandler;
import com.distribute.executor.response.defaultFuture;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Data
public class NettyClient {
//    @Value("${executor.addr}")
    private String addr;

    @Value("${executor.ip:127.0.0.1}")
    private String ip;

    @Value("${executor.port:8092}")
    private Integer port;

    @Value("${executor.name:executor_admin}")
    private String name;

    @Value("${remoting.address:127.0.0.1:8088}")
    private String remotingAddress;

    private Map<String,Channel> channelMap=new HashMap<>();

//    private Channel channel;

    private List<String > addressList;

    private final Map<Long, defaultFuture> futureMap = new ConcurrentHashMap<>();

    private ThreadPoolExecutor connectExecutor;

    @PostConstruct
    public void initialize(){
        this.addr=this.ip+":"+this.port;
//        connectExecutor=new ThreadPoolExecutor(3,5,60, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));
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
        log.info("initialize client ===== ok");
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
                            p.addLast(new LoggingHandler(LogLevel.DEBUG));
                            p.addLast(new MessageCodecSharable());
                            p.addLast(new IdleStateHandler(0, 3, 0));
                            p.addLast(new SendJobMessageHandler());
                            p.addLast(new KillJobMessageHandler());
                            p.addLast(new ClientResponseHandler());
                            p.addLast(new ChatClientHandler(name, addr));
                        }
                    });
            addressList = DataUtil.transferString(remotingAddress);
            for (String address : addressList) {
                ChannelFuture f = b.connect(address.split(":")[0], Integer.parseInt(address.split(":")[1]));
                f.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            System.out.println("connect success " + address + " " + Thread.currentThread());
                        } else {
                            //todo 二次重连
                            System.out.println("connect error " + address + " " + Thread.currentThread());
                        }
                    }
                });
                f.sync();
                channelMap.put(address, f.channel());
                // 发送注册请求
                sendMessage(address, new RegisterInMessage(addr, name, 5), 0);
                log.info("连接remoting{}成功", address);
                // 7.等待连接关闭（阻塞，直到Channel关闭）
//                f.channel().closeFuture().sync();
            }
        }
        finally {
//            group.shutdownGracefully();
        }
    }

    //3次重试，还失败就记录日志
    public void sendMessage(String addr,Message msg,Integer time){
        if(time<3){
            Channel channel=channelMap.get(addr);
            if(channel==null)return;
            ChannelFuture channelFuture = channel.writeAndFlush(msg);
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
                        sendMessage(addr,msg,time+1);
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
