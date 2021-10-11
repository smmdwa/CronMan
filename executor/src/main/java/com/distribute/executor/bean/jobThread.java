package com.distribute.executor.bean;

import com.distribute.executor.invoker.jobInvoker;
import com.distribute.executor.invoker.jobSpringInvoker;
import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.netty_client.execController;
import com.distribute.executor.utils.Context;
import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.SendJobMessage;
//import com.distribute.remoting.bean.Invocation;
import com.distribute.remoting.bean.ResultEnum;
import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.utils.DataUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class jobThread extends Thread {

//    @Autowired
//    jobSpringInvoker invoker;

    private jobHandler handler;

    private Long jobId;

    private LinkedBlockingQueue<SendJobMessage> msgQueue;

    private boolean stop=false;

    private jobBean job;

    private String executorName;

    //线程空等次数
    private Integer waitCount;

    public jobThread(Long jobId){
        this.jobId=jobId;
        msgQueue=new LinkedBlockingQueue<>();
        this.setName("jobThread-"+jobId);
        NettyClient client = (NettyClient) Context.getBean(NettyClient.class);
        this.executorName=client.getName();
        log.info("executorName:"+this.executorName);
    }

    @Override
    public void run() {

        waitCount=0;
        while (!stop){
            boolean ok=true;
            waitCount++;
            SendJobMessage msg=null;
            try {
                //拿到msgQueue
                msg = msgQueue.poll(3L, TimeUnit.SECONDS);

                if(msg!=null){
                    log.info("msg:"+msg);
                    waitCount=0;
                    //填充各种属性，包括context等
                    if(!fulfillProperty(msg)){
                        log.info("参数错误");
                        return;
                    }

                    //初始化
                    this.handler.init();

                    //给handler执行
                    this.handler.execute();

                    //销毁
                    this.handler.destroy();


                    log.info("execute over success");

                }else{
                    //线程空等3秒，累计100次就注销线程
                    if(waitCount>100){
                        jobInvoker.removeJobThread(this.jobId);
                        log.info("removeJobThread");
                        waitCount=0;
                    }
                }
            } catch (Exception e) {
                ok=false;
                log.info("execute over fail");
                log.error(e.getMessage());
            }finally {
                if(ok&&msg!=null){
                    callBackThread.pushCallBack(new CallBackMessage(this.executorName, ResultEnum.success.result,msg.getShardIndex(),msg.getShardTotal(),jobId,msg.getExecId()));
                }else if(!ok&&msg!=null){
                    //todo 失败了 添加报警
                    callBackThread.pushCallBack(new CallBackMessage(this.executorName,ResultEnum.error.result,msg.getShardIndex(),msg.getShardTotal(),jobId,msg.getExecId()));
                }
            }
        }
    }

    public boolean fulfillProperty(SendJobMessage msg){
        jobBean job = msg.getJob();
        this.job=job;
//        Invocation invocation = job.getInvocation();

        //设置线程上下文 用于分片等
        Integer index = msg.getShardIndex();
        Integer total = msg.getShardTotal();
        threadContext.setXxlJobContext(new threadContext(job.getJobId(),index,total));

        //设置handler，用于进行任务execute
        String className = job.getClassName();
        String methodName = job.getMethodName();
        List<String> parameterTypes= DataUtil.transferString(job.getParameterTypes());
        List<String> args1 = DataUtil.transferString(job.getArgs());
        List<Object>realArgs=new ArrayList<>();
        for (int i=0;i<parameterTypes.size();i++) {
            String parameterType=parameterTypes.get(i);
            try {
                Class<?> aClass = Class.forName(parameterType);
                if (String.class.equals(aClass)) {
                    realArgs.add(args1.get(i));
                } else if (Integer.class.equals(aClass)) {
                    realArgs.add(Integer.valueOf(args1.get(i)));
                }else if(Long.class.equals(aClass)){
                    realArgs.add(Long.valueOf(args1.get(i)));
                }else if(Double.class.equals(aClass)){
                    realArgs.add(Double.valueOf(args1.get(i)));
                }else if(boolean.class.equals(aClass)){
                    if("true".equals(args1.get(i)))
                        realArgs.add(true);
                    else if("false".equals(args1.get(i)))
                        realArgs.add(false);
                    else return false;
                }else {
                    //其他类型暂不支持
                    return false;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        methodJobHandler jobHandler = (methodJobHandler)jobInvoker.loadJobHandler(methodName);
        if(jobHandler !=null){
            jobHandler.setArgs(realArgs);
            this.handler=jobHandler;
        }
        return true;
    }

    public void stopJob(){
        this.stop=true;
        this.interrupt();
    }
}
