package com.distribute.executor.invoker;

import com.distribute.executor.annotation.scheduleJob;
import com.distribute.executor.bean.jobHandler;
import com.distribute.executor.bean.jobThread;
import com.distribute.executor.bean.methodJobHandler;
import com.distribute.executor.netty_client.NettyClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Data
public class jobInvoker {

    public void start(){}

    public void destroy(){}

    public static ConcurrentMap<String, jobHandler> jobHandlerRepository = new ConcurrentHashMap<String, jobHandler>();
    public static jobHandler loadJobHandler(String name){
        return jobHandlerRepository.get(name);
    }
    public static jobHandler registJobHandler(String name, jobHandler jobHandler){
        return jobHandlerRepository.put(name, jobHandler);
    }
    protected void registJobHandler(scheduleJob scheduleJob, Object bean, Method executeMethod){
        if (scheduleJob == null) {
            return;
        }

        String name = scheduleJob.name();
        //make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();


//        if (name.trim().length() == 0) {
//            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
//        }
//        if (loadJobHandler(name) != null) {
//            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
//        }


        executeMethod.setAccessible(true);

        // init and destroy
        Method initMethod = null;
        Method destroyMethod = null;

//        if (scheduleJob.init().trim().length() > 0) {
//            try {
//                initMethod = clazz.getDeclaredMethod(scheduleJob.init());
//                initMethod.setAccessible(true);
//            } catch (NoSuchMethodException e) {
//                throw new RuntimeException("scheduleJob-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
//            }
//        }
//        if (scheduleJob.destroy().trim().length() > 0) {
//            try {
//                destroyMethod = clazz.getDeclaredMethod(scheduleJob.destroy());
//                destroyMethod.setAccessible(true);
//            } catch (NoSuchMethodException e) {
//                throw new RuntimeException("scheduleJob-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
//            }
//        }

        // registry jobhandler
        registJobHandler(name, new methodJobHandler(bean, executeMethod, initMethod, destroyMethod));

        log.info("jobHandlerRepository: "+jobHandlerRepository);
    }


    private static ConcurrentMap<Long, jobThread> jobThreadRepository = new ConcurrentHashMap<Long, jobThread>();
    public static jobThread registJobThread(long jobId){
        jobThread newJobThread = new jobThread(jobId);
        newJobThread.start();
        jobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.stopJob();
        }

        return newJobThread;
    }
    public static boolean removeJobThread(long jobId){
        jobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.stopJob();
            return true;
        }
        return false;
    }
    public static jobThread loadJobThread(long jobId){
        jobThread jobThread = jobThreadRepository.get(jobId);
        return jobThread;
    }
    public static void removeAllJobThread(){
        boolean result=true;
        for (Map.Entry<Long, jobThread> entry : jobThreadRepository.entrySet()) {
            Long key = entry.getKey();
            removeJobThread(key);
        }
    }
}
