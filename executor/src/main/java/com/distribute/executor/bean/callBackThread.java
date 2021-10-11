package com.distribute.executor.bean;

import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.utils.Context;
import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.netty_server.nameServerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class callBackThread extends Thread{

    private LinkedBlockingQueue<CallBackMessage> callBackQueue;

    private boolean stop=false;
    private NettyClient client;
    public callBackThread(){
        callBackQueue=new LinkedBlockingQueue<>();
        this.setName("callBackThread:");
        this.setDaemon(true);
        this.start();
        this.client = (NettyClient) Context.getBean(NettyClient.class);
    }

    private volatile static callBackThread instance;

    public static callBackThread getInstance() {
        if (instance == null) {
            synchronized (callBackThread.class) {
                if (instance == null) {
                    instance = new callBackThread();
                }
            }
        }
        return instance;
    }

    public static void pushCallBack(CallBackMessage msg){
        getInstance().callBackQueue.add(msg);
    }


    @Override
    public void run() {
        while (!stop) {

            log.info("callBackThread run");
            //drainto方法可以批量获取数据，但是它无法阻塞，如果队列为空就返回了
            //因为drainto方法不阻塞，所以需要借用take方法的阻塞性  然后再塞回去
            try {
                CallBackMessage msg = this.callBackQueue.take();
                // 拿到要发送的msg
                List<CallBackMessage> callbackParamList = new ArrayList<>();
                this.callBackQueue.drainTo(callbackParamList);
                callbackParamList.add(msg);
                // 发送msg
                for(CallBackMessage message:callbackParamList){
                    this.client.sendMessage(message,0);
                    log.info("send CallBackMessage success msg:"+message);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                log.info("send CallBackMessage error");
            }
        }
    }

    // todo  callback线程需要销毁吗？
    public void stopCallBack(){
        stop=true;
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("callBackThread stop");
    }

}
