package com.distribute.executor.bean;

import com.distribute.executor.invoker.jobInvoker;
import com.distribute.executor.netty_client.NettyClient;
import com.distribute.executor.utils.Context;
import com.distribute.executor.Message.CallBackMessage;
import com.distribute.executor.Message.SendJobMessage;
//import com.distribute.remoting.bean.Invocation;
import com.distribute.executor.utils.DataUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class jobThread extends Thread {

//    @Autowired
//    jobSpringInvoker invoker;

    private Worker handler;

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
    }

    @Override
    public void run() {

        waitCount=0;
        while (!stop){
            boolean ok=true;
            waitCount++;
            SendJobMessage msg=null;
            int execute=0;
            try {
                //拿到msgQueue
                msg = msgQueue.poll(3L, TimeUnit.SECONDS);

                if(msg!=null){
                    log.info("getmsg:"+msg);
                    waitCount=0;
                    //填充各种属性，包括context等
                    if(jobBean.java_normal.equals(msg.getJob().getJobType())||jobBean.java_passive.equals(msg.getJob().getJobType())) {
                        if(!fillMethodProp(msg)){
                            log.info("参数错误");
                            return;
                        }
                    }else{
                        if(!fillShellProp(msg)){
                            log.info("参数错误");
                            return;
                        }
                    }
                    //初始化
                    this.handler.init();

                    //给handler执行
                    execute = this.handler.execute();

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
                e.printStackTrace();
                log.info("execute over fail");
                log.error(e.getMessage());
            }finally {
                if(execute==0)ok=false;
                if(ok){
                    Backer.pushCallBack(new CallBackMessage(this.executorName, ResultEnum.success.result,msg.getShardIndex(),msg.getShardTotal(),jobId,msg.getExecId()));
                }else if(msg != null){
                    //todo 失败了 添加报警
                    Backer.pushCallBack(new CallBackMessage(this.executorName,ResultEnum.error.result,msg.getShardIndex(),msg.getShardTotal(),jobId,msg.getExecId()));
                }
            }
        }
    }

    public boolean fillMethodProp(SendJobMessage msg){
        jobBean job = msg.getJob();
        this.job=job;
//        Invocation invocation = job.getInvocation();

        //设置线程上下文 用于分片等
        Integer index = msg.getShardIndex();
        Integer total = msg.getShardTotal();
        threadContext.setContext(new threadContext(job.getJobId(),index,total));

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
        methodWorker jobHandler = (methodWorker)jobInvoker.getWorker(methodName);
        if(jobHandler !=null){
            jobHandler.setArgs(realArgs);
            this.handler=jobHandler;
            return true;
        }
        return false;
    }

    public boolean fillShellProp(SendJobMessage msg){
        jobBean job = msg.getJob();
        this.job=job;
//        Invocation invocation = job.getInvocation();

        //设置线程上下文 用于分片等
        Integer index = msg.getShardIndex();
        Integer total = msg.getShardTotal();
        threadContext.setContext(new threadContext(job.getJobId(),index,total));

        //String shell
        String shell=replace(job.getShell());
        //设置handler，用于进行任务execute
        List<String> parameterTypes= DataUtil.transferString(job.getParameterTypes());
        List<String> args1 = DataUtil.transferString(job.getArgs());
        //和普通Java任务不一样，ShellWorker每次都需要重新注册，因为Shell任务是可变的，
        //需要根据每次发来的Shell请求内容变化
        ShellWorker shellWorker = new ShellWorker(jobId,shell,index,total,args1);
        jobInvoker.registWorker(String.valueOf(jobId),shellWorker);
        this.handler=shellWorker;
        return true;
    }
    //替换换行符
    private static String replace(String shellValue){
        if(shellValue==null||shellValue.length()==0)return shellValue;
        if(System.getProperty ("os.name").contains("Windows")){
            return shellValue.replaceAll("\n","\r\n");
        }else if(System.getProperty ("os.name").contains("Linux")){
            return shellValue.replaceAll("\n","\r");
        }else if(System.getProperty ("os.name").contains("Mac")){
            return shellValue.replaceAll("\n","\n");
        }
        return shellValue;
    }
    public void stopJob(){
        this.stop=true;
        this.interrupt();
    }
}
