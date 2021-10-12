package com.distribute.executor.bean;

import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.utils.Context;
import com.distribute.executor.utils.DataUtil;
import com.distribute.executor.utils.serialUtil;
import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.ResponseMessage;
import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.bean.returnMSG;
import com.distribute.remoting.response.defaultFuture;
import com.distribute.remoting.utils.FutureUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class backer {

    private LinkedBlockingQueue<CallBackMessage> callBackQueue;

    private boolean stop=false;
    private NettyClient client;
    private Thread sender;
    private Thread watcher;
    private final String fileDir;
    private final String fileName;
    private static final Long watcherSleepTime=30 *1000L;

    public backer(){
        callBackQueue=new LinkedBlockingQueue<>();
        this.client = (NettyClient) Context.getBean(NettyClient.class);
        this.fileDir="data/need-to-back/"+this.client.getName()+"/";
        this.fileName=fileDir+"{temp}"+".log";
    }

    private volatile static backer instance;

    public static backer getInstance() {
        if (instance == null) {
            synchronized (backer.class) {
                if (instance == null) {
                    instance = new backer();
                }
            }
        }
        return instance;
    }

    public void start(){
        sender=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop) {

                    log.info("callBackThread run");
                    //drainto方法可以批量获取数据，但是它无法阻塞，如果队列为空就返回了
                    //因为drainto方法不阻塞，所以需要借用take方法的阻塞性  然后再塞回去
                    try {
                        CallBackMessage msg = backer.this.callBackQueue.take();
                        // 拿到要发送的msg
                        List<CallBackMessage> callbackParamList = new ArrayList<>();
                        backer.this.callBackQueue.drainTo(callbackParamList);
                        callbackParamList.add(msg);
                        // 发送msg
                        for(CallBackMessage message:callbackParamList){
                            tryToCallBack(message);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        log.info("send CallBackMessage error");
                    }
                }
            }
        });
        sender.setName("sender");
        sender.setDaemon(true);
        sender.start();

        watcher=new Thread(new Runnable(){
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(backer.watcherSleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    watchFileToSend();
                }
            }
        });
        watcher.setName("watcher");
        watcher.setDaemon(true);
        watcher.start();

    }

    public static void pushCallBack(CallBackMessage msg){
        getInstance().callBackQueue.add(msg);
    }


    // todo  sender线程需要销毁吗？
    public void stopSender(){
        stop=true;
        sender.interrupt();
        try {
            sender.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("sender stop");
    }


    private void setWatchFile(CallBackMessage message){
        // append file
        byte[] bytes = serialUtil.serialize(message);

        File watchFile = new File(fileName.replace("{temp}", String.valueOf(System.currentTimeMillis())+new Random().nextInt(100)));
        while (watchFile.exists()) {
            watchFile = new File(fileName.replace("{temp}", String.valueOf(System.currentTimeMillis())+new Random().nextInt(1000)));
        }
        DataUtil.setFileContent(watchFile, bytes);

    }

    private void watchFileToSend(){
        File watchFile = new File(fileDir);
        if (!watchFile.exists()) {
            return;
        }
        if (watchFile.isFile()) {
            watchFile.delete();
        }
        if (!(watchFile.isDirectory() && watchFile.list()!=null && watchFile.list().length>0)) {
            return;
        }

        for (File file: watchFile.listFiles()) {
            byte[] bytes = DataUtil.getFileContent(file);
            if(bytes == null || bytes.length < 1){
                file.delete();
                continue;
            }
            CallBackMessage msg = (CallBackMessage) serialUtil.deserialize(bytes, CallBackMessage.class);
            log.info("need to back:"+msg);
            file.delete();
            tryToCallBack(msg);
        }

    }

    private void tryToCallBack(CallBackMessage msg){
        boolean result = false;
        defaultFuture future = new defaultFuture();

        Map<Long, defaultFuture> futureMap = backer.this.client.getFutureMap();
        futureMap.put(msg.getRequestId(),future);

        for(String address:this.client.getAddressList()){
            //发送消息
            backer.this.client.sendMessage(address,msg,0);

            //等待响应
            ResponseMessage responseMessage = FutureUtil.getFuture(futureMap,msg.getRequestId());
            if(responseMessage==null||responseMessage.getCode()==ResponseMessage.error){
                //超时或者任务失败 需要进行持久化
                log.info("callBack job fail");
            }else {
                result=true;
                log.info("send CallBackMessage success msg:"+msg);
                break;
            }
        }
        if(!result){
            setWatchFile(msg);
        }
    }

    public static void main(String[] args) {
//        backer instance = backer.getInstance();
//        System.out.println(instance.fileName);
//        System.out.println(instance.fileDir);
//        instance.setWatchFile(new CallBackMessage());
//        instance.watchFileToSend();
    }

}
