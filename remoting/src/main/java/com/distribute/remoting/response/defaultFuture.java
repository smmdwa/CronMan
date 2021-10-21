package com.distribute.remoting.response;

import com.distribute.remoting.Message.ResponseMessage;

public class defaultFuture {
    private ResponseMessage response;
    private volatile boolean isSucceed = false;
    private final Object object = new Object();

    //超时就退出
    public ResponseMessage getResponse(long timeout) {
        long begin = System.currentTimeMillis();
        //等待间隔，每隔一段时间就查看isSucceed是否为true
//        long waitBeat=timeout/10;
        synchronized (object) {
            //todo 是否要轮询呢，dubbo里是轮询了的，但是我感觉不需要？
//            while (!isSucceed) {
//                try {
//                    object.wait(timeout);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            try {
                object.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    public void setResponse(ResponseMessage response) {
        if (isSucceed) {
            return;
        }
        synchronized (object) {
            this.response = response;
            this.isSucceed = true;
            object.notify();
        }
    }
//
//    public static void main(String[] args) throws InterruptedException {
//        defaultFuture defaultFuture = new defaultFuture();
//        defaultFuture.getResponse(5000);
////        Thread.sleep(1000);
//
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("res");
//                defaultFuture.setResponse(new ResponseMessage());
//                System.out.println("res over");
//            }
//        });
//        thread.start();
//        thread.join();
//        System.out.println("succ:"+defaultFuture.isSucceed);
//    }
}