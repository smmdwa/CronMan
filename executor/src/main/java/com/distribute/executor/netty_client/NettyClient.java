package com.distribute.executor.netty_client;

import com.distribute.executor.Message.ResponseMessage;
import com.distribute.executor.bean.backer;
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
import com.distribute.executor.utils.FutureUtil;
import com.distribute.executor.utils.idUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private List<String > addressList=new ArrayList<>();

    //锁住 addressList和channelMap 发送消息的时候上读锁，断线、上线的时候上写锁修改addressList
    private ReentrantReadWriteLock addressLock= new ReentrantReadWriteLock();

    private final Map<Long, defaultFuture> futureMap = new ConcurrentHashMap<>();

    private ThreadPoolExecutor sendExecutor;

    @PostConstruct
    public void initialize(){
        this.addr=this.ip+":"+this.port;
        sendExecutor=new ThreadPoolExecutor(2,5,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));
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
            List<String>tempAddressList=DataUtil.transferString(remotingAddress);
            try{
                addressLock.writeLock().lockInterruptibly();
                for (String address : tempAddressList) {
                    ChannelFuture f = b.connect(address.split(":")[0], Integer.parseInt(address.split(":")[1]));
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            if (channelFuture.isSuccess()) {
                                final boolean[] res = {false};
                                // big bug  这里必须要新开一个线程，不然就不是同步等待了，
                                // 因为此线程和 ClientResponseHandler 的线程是同一个线程
                                sendExecutor.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean res =register(f.channel());
                                        if(res){
                                            //加入addressList中
                                            channelMap.put(address, f.channel());
                                            addressList.add(address);
                                            log.info("连接remoting{}成功", address);
                                        }
                                    }
                                });
                            }
                        }
                    });
                    f.sync();
                    log.info("连接remoting{}", address);
                }
            }catch (Exception e ){
                log.info(e.getMessage());
            }
            finally {
                addressLock.writeLock().unlock();
            }
        }
        finally {
//            group.shutdownGracefully();
        }
    }

    private boolean register(Channel channel) {

        //设置requestId和futureMap
        defaultFuture future = new defaultFuture();
        RegisterInMessage msg = new RegisterInMessage(addr, name, 5, new idUtil().nextId());
        this.futureMap.put(msg.getRequestId(), future);
        log.info("regi:"+msg.getRequestId());
        // 发送注册请求
        sendMessage(channel, msg, 0);

        //等待响应
        ResponseMessage responseMessage = FutureUtil.getFuture(this.futureMap, msg.getRequestId());
        if (responseMessage == null || responseMessage.getCode() == ResponseMessage.error) {
            log.info("register fail");
            return false;
        }else{
            log.info("register success");
            return true;
        }
    }
    //将断联的调度器剔除map和list中
    public void removeScheduler(Channel channel){
        String address=null;
        try {
            this.addressLock.writeLock().lockInterruptibly();
            for (Map.Entry<String, Channel> entry : this.getChannelMap().entrySet()) {
                if(entry.getValue()==channel){
                    address=entry.getKey();
                    break;
                }
            }
            if(address!=null){
                log.info("removeScheduler:"+address);
                this.addressList.remove(address);
                this.channelMap.remove(address);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.addressLock.writeLock().unlock();
        }
    }

    //3次重试，还失败就记录日志
    public void sendMessage(String addr,Message msg,Integer time){
        Channel channel=null;
        try {
            addressLock.readLock().lockInterruptibly();
            channel=channelMap.get(addr);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            addressLock.readLock().unlock();
        }
        if(channel==null)return;
        doSendMessage(channel,msg,time);
    }
    public void sendMessage(Channel channel,Message msg,Integer time) {
        if(channel==null)return;
        doSendMessage(channel,msg,time);
    }
    private void doSendMessage(Channel channel,Message msg,Integer time){
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
                        future.cause().printStackTrace();
                        doSendMessage(channel,msg,time+1);
                    }
                }
            });
        }else{
            //todo 进行报警等其他操作
            log.info("send msg error >= 3 times CARE");
        }
    }

//    public static void main(String[] args) throws InterruptedException {
//        ReentrantReadWriteLock lock= new ReentrantReadWriteLock();
//
//        try {
//            lock.writeLock().lockInterruptibly();
//            System.out.println("lock1");
//            Reentrant(lock);
//
//        }finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    public static void Reentrant(ReentrantReadWriteLock lock) throws InterruptedException {
//        lock.writeLock().lockInterruptibly();
//        System.out.println("lock2");
//        lock.readLock().lockInterruptibly();;
//        System.out.println("lock33");
//        lock.readLock().unlock();;
//        lock.writeLock().unlock();;
//    }
}
