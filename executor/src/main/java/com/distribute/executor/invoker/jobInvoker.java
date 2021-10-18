package com.distribute.executor.invoker;

import com.distribute.executor.annotation.scheduleJob;
import com.distribute.executor.bean.Worker;
import com.distribute.executor.bean.jobThread;
import com.distribute.executor.bean.methodWorker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Data
public class jobInvoker {

    //todo ? 这里是总入口，所有的component都要从这里初始化
    public void start(){

    }

    public void destroy(){}

    public static ConcurrentMap<String, Worker> workerMaps = new ConcurrentHashMap<String, Worker>();
    public static Worker getWorker(String name){
        return workerMaps.get(name);
    }
    public static Worker registWorker(String name, Worker jobHandler){
        return workerMaps.put(name, jobHandler);
    }
    protected void registAnnotationWorker(scheduleJob scheduleJob, Object bean, Method executeMethod){
        if (scheduleJob == null) {
            return;
        }

        String name = scheduleJob.name();
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        executeMethod.setAccessible(true);

        Method initMethod = null;
        Method destroyMethod = null;

        registWorker(name, new methodWorker(bean, executeMethod, initMethod, destroyMethod));

        log.info("workerMaps: "+workerMaps);
    }


    private static ConcurrentMap<Long, jobThread> threadMaps = new ConcurrentHashMap<Long, jobThread>();
    public static jobThread registJobThread(long jobId){
        jobThread newJobThread = new jobThread(jobId);
        newJobThread.start();
        jobThread oldJobThread = threadMaps.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.stopJob();
        }

        return newJobThread;
    }
    public static boolean removeJobThread(long jobId){
        jobThread oldJobThread = threadMaps.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.stopJob();
            return true;
        }
        return false;
    }
    public static jobThread loadJobThread(long jobId){
        jobThread jobThread = threadMaps.get(jobId);
        return jobThread;
    }
    public static void removeAllJobThread(){
        boolean result=true;
        for (Map.Entry<Long, jobThread> entry : threadMaps.entrySet()) {
            Long key = entry.getKey();
            removeJobThread(key);
        }
    }
}
