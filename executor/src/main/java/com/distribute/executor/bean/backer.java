package com.distribute.executor.bean;

import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.utils.*;
import com.distribute.executor.Message.CallBackMessage;
import com.distribute.executor.Message.ResponseMessage;
import com.distribute.executor.response.defaultFuture;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
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
        this.fileDir="./data/need-to-back/"+this.client.getName()+"/";
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
            log.info("path:{},file:{}",file.getAbsolutePath(),file.getAbsoluteFile());
            file.delete();
            tryToCallBack(msg);
        }

    }

    private void tryToCallBack(CallBackMessage msg){
        log.info("tryToCallBack:{} ",msg);
        boolean result = false;
        List<String>addressList=new ArrayList<>();
        try {
            this.client.getAddressLock().readLock().lockInterruptibly();
            addressList=this.client.getAddressList();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.client.getAddressLock().readLock().unlock();
        }
        for(String address:addressList){
            //设置requestId和futureMap
            //每次都要重新设置，因为每次都会清空
            msg.setRequestId(new idUtil().nextId());
            defaultFuture future = new defaultFuture();
            Map<Long, defaultFuture> futureMap = backer.this.client.getFutureMap();
            futureMap.put(msg.getRequestId(),future);

            //发送消息
            backer.this.client.sendMessage(address,msg,0);

            //等待响应
            ResponseMessage responseMessage = FutureUtil.getFuture(backer.this.client.getFutureMap(),msg.getRequestId());
            if(responseMessage==null||responseMessage.getCode()==ResponseMessage.error){
                //超时或者任务失败 需要进行持久化
                log.info("callBack job fail");
            }else {
                result=true;
                log.info("send CallBackMessage success msg:{} id:{}",msg,msg.getRequestId());
                break;
            }
            log.info("address:{} 发送失败 ",address);
        }
        if(!result){
            setWatchFile(msg);
        }
    }

//    public static void main(String[] args) throws IOException {
////        backer instance = backer.getInstance();
////        System.out.println(instance.fileName);
////        System.out.println(instance.fileDir);
////        instance.setWatchFile(new CallBackMessage());
////        instance.watchFileToSend();
////        Process process = Runtime.getRuntime().exec("sh echo 111\necho hello");
////        printResults(process);
//        System.out.println (System.getProperty ("os.name"));
//        System.out.println(System.getProperty("os.name").contains("Windows"));
//        markScriptFile("data/shell/11.sh","#!/bin/bash\\necho 111\\necho hello");
//    }
//    //替换换行符
//    public static String replace(String shellValue){
//        if(System.getProperty ("os.name").contains("Windows")){
//            return shellValue.replaceAll("\\n","\r\n");
//        }else if(System.getProperty ("os.name").contains("Linux")){
//            return shellValue.replaceAll("\\n","\r");
//        }else if(System.getProperty ("os.name").contains("Mac")){
//            return shellValue.replaceAll("\\n","\n");
//        }
//        return shellValue;
//    }
//    public static void printResults(Process process) throws IOException {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        String line = "";
//        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
//        }
//    }
//    public static void markScriptFile(String scriptFileName, String content) throws IOException {
//        // make file,   filePath/gluesource/666-123456789.py
//        FileOutputStream fileOutputStream = null;
//        try {
//            fileOutputStream = new FileOutputStream(scriptFileName);
//            content=replace(content);
//            fileOutputStream.write(content.getBytes("UTF-8"));
//            fileOutputStream.close();
//        } catch (Exception e) {
//            throw e;
//        }finally{
//            if(fileOutputStream != null){
//                fileOutputStream.close();
//            }
//        }
//    }

}
