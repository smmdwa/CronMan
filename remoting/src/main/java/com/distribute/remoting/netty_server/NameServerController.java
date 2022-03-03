package com.distribute.remoting.netty_server;


import com.distribute.remoting.DAG.Dag;
import com.distribute.remoting.DAG.NodeDAG;
import com.distribute.remoting.DAG.RelationDAG;
import com.distribute.remoting.DAG.ResultDAG;
import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.KillJobMessage;
import com.distribute.remoting.Message.Message;
import com.distribute.remoting.Message.ResponseMessage;
//import com.distribute.remoting.annotation.scheduleJob;
import com.distribute.remoting.bean.*;
import com.distribute.remoting.mapper.JobMapper;
import com.distribute.remoting.response.defaultFuture;
import com.distribute.remoting.thread.sendJobThread;
import com.distribute.remoting.utils.*;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
//import org.springframework.scheduling.support.CronExpression;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@Slf4j
@Component
@DependsOn({"routeInfoManager", "context"})
public class NameServerController {
//    private volatile static nameServerController instance;

    @Resource
    DataSource dataSource;

    @Autowired
    JobMapper mapper;

    //等于routemanager的dataLock
    private ReadWriteLock lock ;

    @Autowired
    RouteInfoManager routemanager;
//    private final routeInfoManager routemanager=routeInfoManager.getInstance();
    @Autowired
    NettyServer server;

    //用来控制toBeRingThread的唤醒和睡眠
    private final Object obj=new Object();

    private final  ConcurrentHashMap<Long, jobExecInfo> jobTable=new ConcurrentHashMap<>(128);

    private ThreadPoolExecutor sendExecutor;

    private ThreadPoolExecutor callBackExecutor;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "Scan_Executor_Thread"));

    private final ScheduledExecutorService scheduledLookingFailedTask = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "Scan_FailedTask_Thread"));

    private final int nthreads= 5;

    private final Map<Long, defaultFuture> futureMap = new ConcurrentHashMap<>();

    private volatile Map<Integer,List<Long>> timeRing=new HashMap<>();

    private toBeRunJobThread toBeRunJobThread;

    private toBeRingThread toBeRingThread;



    @PostConstruct
    public void initialize() {
        this.lock=routemanager.getDataLock();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //注意这里会一直卡住，同步等待结束 所以要用一个线程装
                try {
                    NameServerController.this.server.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //定时扫描活跃节点
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                NameServerController.this.routemanager.scanNotActiveExecutor();
            }
        }, 5, 10 , TimeUnit.SECONDS);

        //定时扫描失败的任务：从运行开始，超过一定时间仍未完成则判定为任务失败
        this.scheduledLookingFailedTask.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Scheduled.ScheduleLookingFailedTask();
            }
        },Const.QueryFirstDelay,Const.QueryInterval,TimeUnit.HOURS);

        //专门用来发送sendJobMessage给 executor
        sendExecutor=new ThreadPoolExecutor(2,5,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

        //专门用来处理callBackMessage
        callBackExecutor=new ThreadPoolExecutor(2,5,120,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

        //专门用来发送
        sendJobThread.getInstance();

        toBeRunJobThread = new toBeRunJobThread();
        toBeRunJobThread.setDaemon(true);
        toBeRunJobThread.setName("toBeRunJob");
        toBeRunJobThread.start();

        toBeRingThread = new toBeRingThread();
        toBeRingThread.setDaemon(true);
        toBeRingThread.setName("toBeRing");
        toBeRingThread.start();
        log.info("initial nameserver");
    }

    public void sendResponse(Message msg,String name){
        Channel channel = this.routemanager.getExecutorChannel(name);
        this.server.sendMessage(msg,0,channel);
    }
    public void sendResponse(Message msg, Channel channel){
        this.server.sendMessage(msg,0,channel);
    }

    //添加任务的入口，构造jobBean
    public returnMSG addJobController(String name,String pids,String className,String methodName,String paramType,String params,String cronExpr,Integer shardNum,boolean transfer,boolean reStart,String policy,String jobType,String shell){
        jobBean job;
        long id = new idUtil().nextId();
        //-1代表依赖任务是自己
        if("-1".equals(pids)) pids=String.valueOf(id);
        //是否是主动任务
        if(jobBean.java_normal.equals(jobType)){
            job = new jobBean(id,pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),getNextStartTime(cronExpr),jobBean.init,0,jobType,jobBean.enabled,null);
        }else if(jobBean.shell_normal.equals(jobType)){
            job = new jobBean(id,pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),getNextStartTime(cronExpr),jobBean.init,0,jobType,jobBean.enabled,shell);
        }else if(jobBean.java_passive.equals(jobType)){
            job = new jobBean(id,pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),0L,jobBean.waiting,0,jobType,jobBean.enabled,null);
        }
        else{
            job = new jobBean(id,pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),0L,jobBean.waiting,0,jobType,jobBean.enabled,shell);
        }
        log.info("new job:"+job);
        try {
            lock.writeLock().lockInterruptibly();
            mapper.insertJob(job);

        } catch (Exception e) {
            e.printStackTrace();
            return new returnMSG<ResultDAG>(500,"error",null,0);
        }finally {
            lock.writeLock().unlock();
        }
        return new returnMSG<ResultDAG>(200,"success",null,0);
    }

    //替换换行符
    public static String replace(String shellValue){
        if(System.getProperty ("os.name").contains("Windows")){
            return shellValue.replaceAll("\n","\r\n");
        }else if(System.getProperty ("os.name").contains("Linux")){
            return shellValue.replaceAll("\n","\r");
        }else if(System.getProperty ("os.name").contains("Mac")){
            return shellValue.replaceAll("\n","\n");
        }
        return shellValue;
    }
    //获取任务依赖图的入口
    public returnMSG getDagController(Long jobId){
        try {
            lock.readLock().lockInterruptibly();
            List<jobBean> allJob = mapper.getAllJob();
            //构造dag
            List<NodeDAG>nodes=new ArrayList<>();
            for (jobBean jobBean : allJob) {
                nodes.add(new NodeDAG(jobBean.getName(),jobBean.getJobId()));
            }
            Dag dag = new Dag(nodes);
            //构造dag边
            List<RelationDAG>relations=new ArrayList<>();
            for (jobBean jobBean : allJob) {
                if(jobBean.getPids()==null)continue;
                List<Long> list = DataUtil.transferLong(jobBean.getPids());
                for (Long aLong : list) {
                    if(mapper.getJobById(aLong)==null)continue;
                    dag.addAdj(aLong,jobBean.getJobId());
                    relations.add(new RelationDAG(mapper.getJobById(aLong).getName(),jobBean.getName()));
                }
            }
            //获取依赖图
            dag.getRelatedDAG(jobId);
            HashSet<NodeDAG> nodeDAGS = dag.transferToNodeDag();
            for (NodeDAG nodeDAG : nodeDAGS) {
                nodeDAG.setStatus(mapper.getJobById(nodeDAG.getJobId()).getStatus());
            }
            return new returnMSG<ResultDAG>(200,"success",new ResultDAG(nodeDAGS,relations),0);
        } catch (Exception e) {
            e.printStackTrace();
            return new returnMSG<ResultDAG>(500,"error",null,0);
        }finally {
            lock.readLock().unlock();
        }
    }

    //判断上游任务是否存在
    public boolean isExistTheJob(String pids){
        List<Long> list = DataUtil.transferLong(pids);
        try {
            lock.readLock().lockInterruptibly();
            for (Long aLong : list) {
                jobBean jobById = mapper.getJobById(aLong);
                if(jobById==null)return false;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.readLock().unlock();
        }
        return true;
    }

    public returnMSG<List<jobBean>> getJobController(){
        try {
            lock.readLock().lockInterruptibly();
            List<jobBean> allJob = mapper.getAllJob();
            return new returnMSG<List<jobBean>>(200,"success",allJob,allJob.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new returnMSG<List<jobBean>>(500,"error",null,0);
        }finally {
            lock.readLock().unlock();
        }
    }

    public returnMSG killJob(Long jobId){
        //先判断是否需要kill 如果status为已经停用，就不再调用了
        try {
            lock.writeLock().lockInterruptibly();
            jobBean jobById = mapper.getJobById(jobId);
            if(jobById.getStatus()==jobBean.stopped){
                return new returnMSG<List<jobBean>>(500,"任务已经停用",null,0);
            }

            List<executorLiveInfo> infos = this.routemanager.getAllExecutorInfo(jobId);
            if(infos==null)return new returnMSG<List<jobBean>>(200,"success",null,0);
            //向所有jobId关联的executor 发送消息
            for (executorLiveInfo info : infos) {
                //生成future记录，根据requestId 获取对应结果
                defaultFuture future = new defaultFuture();
                Long requestId=idGenerator.nextLongId();
                this.futureMap.put(requestId,future);

                //发送消息
                this.server.sendMessage(new KillJobMessage(jobId,requestId,this.server.getServerAddress(),info.getExecutorAddr()),0,info.getChannel());

                //等待响应
                ResponseMessage msg = FutureUtil.getFuture(this.futureMap,requestId);
                if(msg==null){
                    //超时 认为任务失败
                    log.info("kill job timeout");
                    return new returnMSG<List<jobBean>>(500,"Kill任务超时",null,0);
                }else {
                    int code = msg.getCode();
                    if(code==ResponseMessage.error){
                        log.info("kill job fail");
                        return new returnMSG<List<jobBean>>(500,"Kill任务错误",null,0);
                    }
                }
            }

            //修改状态为stopped
            mapper.updateJobStatusById(jobId,jobBean.stopped);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.writeLock().unlock();
        }
        return new returnMSG(200,"success",null,0);
    }

    public returnMSG DeleteJob(Long jobId){

        //先killJob 再设置enable=1
        killJob(jobId);

        try {
            lock.writeLock().lockInterruptibly();
            mapper.updateJobDisable(jobId);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new returnMSG(500,"error",null,0);
        }finally {
            lock.writeLock().unlock();
        }
        return new returnMSG(200,"success",null,0);
    }

    public returnMSG StartJob(Long jobId){
        //先判断是否需要start 如果status不为已停止，就不再调用了
        try {
            lock.writeLock().lockInterruptibly();
            jobBean jobById = mapper.getJobById(jobId);
            if(jobById.getStatus()!=jobBean.stopped){
                return new returnMSG(500,"任务已经启用",null,0);
            }
            //修改mapper状态
            if(jobBean.java_passive.equals(jobById.getJobType())||jobBean.shell_passive.equals(jobById.getJobType()))
                mapper.updateJobStatusById(jobId,jobBean.waiting);
            else
                mapper.updateJobStatusById(jobId,jobBean.init);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new returnMSG(500,"error",null,0);
        }finally {
            lock.writeLock().unlock();
        }
        return new returnMSG(200,"success",null,0);
    }

    //启动前 5秒 读取任务
    private final long fetchTime=5 * 1000;

    private int testTime=4;

    //dependNormalTime以内完成的上游任务都认为有效
    private static final Long dependNormalTime=20* 60 *1000L;

    //判断上游任务是否全部完成
    private boolean isDependFinishNormal(String pids){
        try {
            lock.readLock().lockInterruptibly();
            List<Long> list = DataUtil.transferLong(pids);
            for(Long a:list){
                jobBean jobById = mapper.getJobById(a);
                long before=jobById.getUpdateTime().getTime();
                if(System.currentTimeMillis()-before>dependNormalTime){
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.readLock().unlock();
        }
        return true;
    }
    //定时读取数据库将在5s内触发的jobBean放入时间轮中
    //先上写锁，因为要读job表，防止其他线程和此线程竞争
    //再上排他锁，防止另外的调度器和此调度器竞争，避免消息多发
    class toBeRunJobThread extends Thread{
        @Override
        public void run() {
            while (true){
                try {
                    TimeUnit.MILLISECONDS.sleep(fetchTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    lock.writeLock().lockInterruptibly();
                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;
                    try {
                        conn = dataSource.getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        conn.setAutoCommit(false);

                        preparedStatement = conn.prepareStatement(  "select * from jobLock where mylock = 'lock' for update" );
                        preparedStatement.execute();
                        long readTime=System.currentTimeMillis();

                        List<jobBean> jobList = mapper.getToBeRunJob(readTime+fetchTime);

                        if(jobList==null||jobList.size()==0)
                            continue;
                        log.info("jobList: "+jobList);
                        //先进行粗略的判断 假设 readTime 95 则list中包含0-100的所有任务
                        // 1. <=90 任务过期 根据错过重触发策略判断是否要触发
                        // 2. 90-95 任务可能因为调度问题而错过上一次的调度
                        // 3. >95 正常放入时间轮
                        for (jobBean job : jobList) {
                            //被动任务直接跳过 依赖任务先判断依赖任务是否全部完成
                            if(jobBean.java_passive.equals(job.getJobType())||jobBean.shell_passive.equals(job.getJobType())) {
                                continue;
                            }else if(job.getPids()!=null&&job.getPids().length()!=0&&!isDependFinishNormal(job.getPids())){
                                continue;
                            }
                            if(job.getNextStartTime()<readTime-fetchTime){
                            log.info("过期："+job);
                            //1. <=90 任务过期
                            if(job.isRestart()){
                                sendJobToExecutor(job);
                                job.setNextStartTime(getNextStartTime(job.getCronExpr()));
                            }
                            }else if(job.getNextStartTime()<readTime){
                                //2. 90-95
                                log.info("可发送："+job);
                                sendJobToExecutor(job);
                                job.setNextStartTime(getNextStartTime(job.getCronExpr()));
                                if(job.getNextStartTime()<readTime+fetchTime){
                                    //放入时间轮中
                                    int ring = (int)((job.getNextStartTime()/1000)%60);
                                    putIntoTimeRing(ring,job.getJobId());
                                    job.setNextStartTime(getNextTwoTime(job.getCronExpr()));
                                    log.info("放入时间轮："+job);
                                }
                            }else{
                                //3. >95 正常放入时间轮
                                int ring = (int)((job.getNextStartTime()/1000)%60);
                                putIntoTimeRing(ring,job.getJobId());
                                job.setNextStartTime(getNextTwoTime(job.getCronExpr()));
                                log.info("放入时间轮："+job);
                            }
                        }
                        //修改job 主要是修改nextStartTime来避免竞争
                        //修改jobTable 更改状态
                        for (jobBean job : jobList) {
                            if(jobBean.java_passive.equals(job.getJobType())||jobBean.shell_passive.equals(job.getJobType()))
                                continue;
                            job.setStatus(jobBean.doing);
                            mapper.update(job);
                        }
                        log.info("更改完成");

                        //如果时间轮里有任务，并且toBeRingThread的状态是waited，就唤醒它
                        synchronized (obj){
                            if(!timeRing.isEmpty()&&toBeRingThread.getState().equals(Thread.State.WAITING)){
                                log.info("唤醒toBeRingThread");
                                obj.notify();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }finally {
                        // 关闭排他锁
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                            }
                            try {
                                conn.setAutoCommit(connAutoCommit);
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                            }
                        }

                        // close PreparedStatement
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    lock.writeLock().unlock();
                }
            }

        }
    }

    //放入时间轮
    public void putIntoTimeRing(int ring, long jobId){
        List<Long> ringItemData = timeRing.computeIfAbsent(ring, k -> new ArrayList<Long>());
        ringItemData.add(jobId);
    }

    class toBeRingThread extends Thread{
        @Override
        public void run() {
            while (true){
                // item放着jobId
                List<Long> ringItemData = new ArrayList<>();
                //获取当前时刻的秒数，1秒，2秒
                int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                for (int i = 0; i < 2; i++) {
                    List<Long> tmpData = timeRing.remove( (nowSecond+60-i)%60 );
                    if (tmpData != null) {
                        ringItemData.addAll(tmpData);
                    }
                }
                try {
                    lock.readLock().lockInterruptibly();
                    if (ringItemData.size() > 0) {
                        // 需要发送
                        for (Long jobId : ringItemData) {
                            sendJobToExecutor(mapper.getJobById(jobId));
                        }
                        ringItemData.clear();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    lock.readLock().unlock();
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(1000-100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //如果时间轮里已经处理完毕了，就阻塞，等待唤醒。避免空转
                synchronized (obj) {
                    if(timeRing.isEmpty()){
                        try {
                            log.info("时间轮任务已完成，阻塞等待");
                            obj.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }

    private void sendJobToExecutor(jobBean jojo){
        //推送给executor
        sendExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                NameServerController.this.routemanager.sendJobToExecutor(jojo);
            }
        });
    }

    //返回下一次时间
    public static Long getNextStartTime(String cronExpr) {
        if(cronExpr==null){
            return 0L;
        }
        try {
            CronExpression cronExpression = new CronExpression(cronExpr);
            return cronExpression.getNextValidTimeAfter(new Date()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    //返回下下一次时间
    public static Long getNextTwoTime(String cronExpr)  {

        if(cronExpr==null){
            return 0L;
        }
        try {
            CronExpression cronExpression = new CronExpression(cronExpr);
            return cronExpression.getNextValidTimeAfter(new Date(getNextStartTime(cronExpr))).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    //接受CallBackMessage，维护任务table
    //    1.如果code为200，任务完成，则设置isfinish=true
    //    2.         300, 任务出错，则认定为任务失败
    //    3.         400, executor断线，失效转移，让新的executor完成任务，由controller来做
    public ResponseMessage handleCallBack(CallBackMessage msg){
        String name = msg.getName();
        Long jobId = msg.getJobId();
        Integer index = msg.getShardIndex();
        Integer total = msg.getShardTotal();
        Integer code = msg.getCode();
        Integer execId=msg.getExecId();
        byte[] content = msg.getContent();
        boolean isCompress = msg.isCompress();
        log.info("handleCallBack code:"+code+" jobId:"+jobId);
        try {
            lock.writeLock().lockInterruptibly();
            //修改状态 不需要修改executor_name
            mapper.updateJobDetail(jobId,execId,name,code,"",content,isCompress);
            if(code== ResultEnum.success.result){
                //检查是否全部完成
                if(isFinish(jobId,execId)){
                    //更新jobBean状态
                    jobBean job = mapper.getJobById(jobId);
                    if(jobBean.java_passive.equals(job.getJobType())||jobBean.shell_passive.equals(job.getJobType())){
                        mapper.updateJobFinish(jobId,jobBean.waiting,new Date());
                    }else{
                        mapper.updateJobFinish(jobId,jobBean.init,new Date());
                    }
                    //检查是否有依赖任务可以发送
                    dependFinish();
                    //todo 全部完成 通知并记录，删除此jobId
                }
            }else if(code==ResultEnum.error.result){
                log.info("jobId:"+jobId+" error!");
                //todo 任务失败，报警并记录

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ResponseMessage(msg.getRequestId(),500,"error");
        }finally {
            lock.writeLock().unlock();
        }
        return new ResponseMessage(msg.getRequestId(),200,"success");
    }

    //检查是否全部完成
    private boolean isFinish(Long jobId,Integer execId){
        try {
            lock.readLock().lockInterruptibly();
            List<jobFinishDetail>details=mapper.getJobDetail(jobId,execId);
            if(details==null)return false;
            for (jobFinishDetail detail : details) {
                if(detail.getCode()!=ResultEnum.success.result){
                    return false;
                }
            }
            log.info("isFinish:{} true!",jobId);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }finally {
            lock.readLock().unlock();
        }
    }

    //在dependInTime之内都算任务可以进行
    private static final Long dependInTime=1000L;

    //查找是否有可以执行的依赖任务 只会有一个调度器触发这个函数，所以不需要上数据库的排他锁
    private void dependFinish(){
        try {
            lock.writeLock().lockInterruptibly();
            List<jobBean> allJob = mapper.getAllJob();
            for (jobBean job:allJob) {
                if(job.getStatus()!=jobBean.waiting){
                    continue;
                }
                if(jobBean.java_passive.equals(job.getJobType())||jobBean.shell_passive.equals(job.getJobType())){
                    //为被动依赖任务 判断是否他的pid任务是否全部完成
                    List<Long>pids= DataUtil.transferLong(job.getPids());
                    boolean result=true;
                    for (Long pid : pids) {
                        jobBean fatherJob = mapper.getJobById(pid);
                        long before=fatherJob.getUpdateTime().getTime();
                        if(System.currentTimeMillis()-before>dependInTime){
                            result=false;
                            break;
                        }
                        //还需要判断自己的updateTime是否比依赖任务的早，避免多次触发
                        if(job.getUpdateTime().getTime()>=before){
                            result=false;
                            break;
                        }
                    }
                    if(result){
                        log.info("被动依赖任务发送"+job);
                        //全部完成，可以发送
                        //因为是被动任务，所以不需要设置nextStartTime
                        mapper.updateJobStatusById(job.getJobId(),jobBean.doing);
                        sendJobToExecutor(job);//todo 记得修改jobtable和nextstarttime状态  要上锁
                    }
                }
            }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                lock.writeLock().unlock();
            }
    }

//    public static void main(String[] args) throws InterruptedException, ParseException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

//        jobBean jobBean=new jobBean();
//        jobBean.setCronExpr("10-30 * * 8-31 * ?");

//        System.out.println(getNextStartTime("10-30 * * 8-31 * ?"));
//        System.out.println(getNextTwoTime("10-30 * * 8-31 * ?"));


//        addJob(jobBean);

//        String cron = "0/30 * * * * ?";
//        //spring @since 5.3
//        CronExpression cronExpression = CronExpression.parse(cron);
//        //下次预计的执行时间
//        LocalDateTime nextFirst = cronExpression.next(LocalDateTime.now());
//        System.out.println(nextFirst);


//        nameServerController.getInstance().initialize();
//        NettyServer.getInstance().start();


//        nameServerController controller = nameServerController.getInstance();
//        controller.initialize();
//        controller.server.start();


//        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
//        String scriptText = "class Hello { void hello() { println 'hello' } }";
//        String scriptText1 = "        public class AJavaClass {\n" +
//                "            {\n" +
//                "                System.out.println(\"Created Java Class\");\n" +
//                "            }\n" +
//                "\n" +
//                "            public void hello() { System.out.println(\"hello\"); }\n" +
//                "        }";
//
//
//        //将Groovy脚本解析为Class对象
//        Class clazz = groovyClassLoader.parseClass(scriptText1);
//        //Class clazz = groovyClassLoader.parseClass(new File("script.groovy"));
//        System.out.println("equals:"+"Hello".equals(clazz.getName()));
//        clazz.getMethod("hello").invoke(clazz.newInstance());



//    }

    public static void main(String[] args) {
        temp temp = new temp();
    }
    static class temp{
        final Object obj=new Object();
        List<String>data=new ArrayList<>();
        Thread t1,t2;
        public temp(){
            t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("t1 run");
                        data.add("ff");
                        data.add("ff");
                        synchronized (obj){
                            System.out.println("t2.state:"+t2.getState());
                            if(!data.isEmpty()&&t2.getState().equals(Thread.State.WAITING)){
                                System.out.println("t1 size:"+data.size());
                                obj.notify();
                            }
                        }
                    }
                }
            });
            t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("t2 run");
                        synchronized (obj) {
                            if(data.isEmpty()){
                                try {
                                    System.out.println("t2 size:"+data.size());
                                    obj.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        System.out.println("t2 run");
                        data.remove(0);
                    }
                }
            });
            data.add("ff");
            data.add("ff");
            data.add("ff");
            t1.start();
            t2.start();

        }
    }
}

