package com.distribute.executor.netty_client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//用来管理等待执行的任务
public class jobManager {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 正在执行的任务的Future  如果执行完毕，就发送成功消息  如果执行失败，就发送失败消息
     */
    private Map<String, Future> doingFutures = new HashMap<>();

    private volatile static jobManager instance;

    public static jobManager getInstance() {
        if (instance == null) {
            synchronized (jobManager.class) {
                if (instance == null) {
                    instance = new jobManager();
                }
            }
        }
        return instance;
    }

    public void start(){

    }

}
