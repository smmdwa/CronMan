package com.distribute.executor.netty_client;


import com.distribute.executor.bean.backer;
import com.distribute.executor.bean.jobThread;
import com.distribute.executor.invoker.jobInvoker;
import com.distribute.executor.invoker.jobSpringInvoker;
import com.distribute.executor.Message.KillJobMessage;
import com.distribute.executor.Message.Message;
import com.distribute.executor.Message.ResponseMessage;
import com.distribute.executor.Message.SendJobMessage;
//import com.distribute.remoting.bean.Invocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component
@DependsOn("context") //bean一般都为 首字母小写
public class execController {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private Map<Long, Future> doingFutures = new HashMap<>();

    private final jobManager manager=jobManager.getInstance();

    private ThreadPoolExecutor jobExecutor;

    @Autowired
    NettyClient client;



    @PostConstruct
    public void initialize() throws InterruptedException {

//        new NettyClient().start();
        //定时扫描doingFuture

//        //注册jobInvoker 用来记录任务完成情况
//        invoker= new jobSpringInvoker();

        //执行任务
        jobExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

        backer.getInstance().start();
        log.info("initialize ===== ok");
    }

    public void addNewJob(SendJobMessage msg) {
        long id = msg.getJob().getJobId();
        jobThread thread =null;
        //如果是第一次创建
        if(jobInvoker.loadJobThread(id)==null){
            thread = jobInvoker.registJobThread(id);
        }else{
        //如果有了就根据策略选择：1.如果消息队列里有消息，就丢弃这个消息；如果有就放进去
            //                2.采用这个消息，把之前的消息给取消
            thread=jobInvoker.loadJobThread(id);
            if(strategy()==1){
                if(thread.getMsgQueue().size()>0){
                    //do nothing
                    return;
                }
            }else {
                if(thread.getMsgQueue().size()>0){
                    //register会删除之前的jobThread
                    thread=jobInvoker.registJobThread(id);
                }
            }
        }
        //把消息传送给阻塞队列，等待jobThread消费
        thread.getMsgQueue().add(msg);
    }

    //删除job
    public ResponseMessage killOldJob(KillJobMessage msg){
        long id=msg.getJobId();
        jobThread jobThread = jobInvoker.loadJobThread(id);
        if(jobThread!=null){
            jobInvoker.removeJobThread(id);
            log.info("killed!");
            return new ResponseMessage(msg.getRequestId(),Message.success,"killed");
        }
        log.info("already killed or not exist");
        return new ResponseMessage(msg.getRequestId(),Message.error,"already killed or not exist");
    }

    public void sendResponse(String addr,Message msg){
        client.sendMessage(addr,msg,0);
    }

    //删除所有job
    public void killAllJob(){
        jobInvoker.removeAllJobThread();
    }

    public int strategy(){
        return 1;
    }

//    public boolean stopJob(long jobId){
//        Future future=doingFutures.get(jobId);
//        boolean cancel = future.cancel(true);
//        return cancel;
//    }

//    class executorCall implements Callable<String> {
//
//        private jobBean job;
//
//        public executorCall(jobBean jobBean) {
//            this.job = jobBean;
//        }
//
//        @Override
//        public String call(){
//            try {
//                //开始执行任务
//                Invocation invocation = this.job.getInvocation();
//
//                boolean result = invoke(invocation);
//
//
//            }catch (Exception e){
//                log.info("failure ===== "+e.getMessage());
//                return "failure";
//            }
//            log.info("invoke success");
//            return "success";
//        }
//    }
//
//    public boolean invoke(Invocation invocation ) throws Exception {
//        String className = invocation.getClassName();
//        String methodName = invocation.getMethodName();
//        String[] parameterTypes = invocation.getParameterTypes();
////        Object[] args = invocation.getArgs();
//        Object[]args= new Object[]{"12",111};
//        methodJobHandler jobHandler = (methodJobHandler)invoker.getJobHandlerRepository().get(methodName);
//        if(jobHandler !=null){
//            jobHandler.setArgs(args);
//            jobHandler.execute();
//        }
//        return true;
//    }
    private static InheritableThreadLocal<String> contextHolder = new InheritableThreadLocal<>(); // support for child thread of job handler)


    public static void main(String[] args) throws InterruptedException {
//        execController.getInstance().initialize();
//        new NettyClient().start();


        Runnable r1 = new Runnable() {
            @Override
            public void run() {
                System.out.println("r1");
                contextHolder.set("我是sssssssss");
                System.out.println("r1"+contextHolder.get());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(r1).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("r2");
                System.out.println("r2"+contextHolder.get());
            }
        }).start();
    }


}