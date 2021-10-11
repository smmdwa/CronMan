package com.distribute.remoting.netty_server;


import com.distribute.remoting.DAG.Dag;
import com.distribute.remoting.DAG.NodeDAG;
import com.distribute.remoting.DAG.RelationDAG;
import com.distribute.remoting.DAG.ResultDAG;
import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.KillJobMessage;
import com.distribute.remoting.Message.ResponseMessage;
//import com.distribute.remoting.annotation.scheduleJob;
import com.distribute.remoting.bean.*;
import com.distribute.remoting.mapper.JobMapper;
import com.distribute.remoting.response.defaultFuture;
import com.distribute.remoting.thread.sendJobThread;
import com.distribute.remoting.utils.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
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

@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
@Component
public class nameServerController {
//    private volatile static nameServerController instance;

    @Resource
    DataSource dataSource;

    @Autowired
    JobMapper mapper;

//    public static nameServerController getInstance() {
//        if (instance == null) {
//            synchronized (nameServerController.class) {
//                if (instance == null) {
//                    instance = new nameServerController();
//                }
//            }
//        }
//        return instance;
//    }

    //等于routemanager的dataLock
    private ReadWriteLock lock ;

    @Autowired
    routeInfoManager routemanager;
//    private final routeInfoManager routemanager=routeInfoManager.getInstance();

    private final NettyServer server=NettyServer.getInstance();

//    private final  ConcurrentHashMap<Long, jobExecInfo> jobTable=new ConcurrentHashMap<>(128);

    private ThreadPoolExecutor sendExecutor;

    private ThreadPoolExecutor callBackExecutor;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "schedule-Thread"));

    private final int nthreads= 5;

    private final Map<Long, defaultFuture> futureMap = new ConcurrentHashMap<>();

    private volatile Map<Integer,List<Long>> timeRing=new HashMap<>();


    public ResponseMessage getFuture(Long requestId) {
        try {
            defaultFuture future = futureMap.get(requestId);
            return future.getResponse(2000); //2秒就算超时
        } finally {
            //获取成功以后，从map中移除
            futureMap.remove(requestId);
        }
    }

    public boolean setFuture(ResponseMessage msg) {
        defaultFuture future = futureMap.get(msg.getRequestId());
        if(future==null){
            return false;
        }
        future.setResponse(msg);
        return true;
    }
    @PostConstruct
    public void initialize() {

        this.scheduledExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                //注意这里会一直卡住，同步等待结束 所以要用一个线程装
                try {
                    nameServerController.this.server.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //定时扫描活跃节点
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                nameServerController.this.routemanager.scanNotActiveExecutor();
            }
        }, 5, 10 , TimeUnit.SECONDS);

//        //定时扫描jobTable
//        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                //                    scanJobTable();
//                toBeRunJob();
//            }
//        }, 5, 5 , TimeUnit.SECONDS);


        //专门用来发送sendJobMessage给 executor
        sendExecutor=new ThreadPoolExecutor(5,10,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

        //专门用来处理callBackMessage
        callBackExecutor=new ThreadPoolExecutor(10,10,120,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));

        //专门用来发送
        sendJobThread.getInstance();

        toBeRunJobThread toBeRunJobThread = new toBeRunJobThread();
        toBeRunJobThread.setDaemon(true);
        toBeRunJobThread.setName("toBeRunJob");
        toBeRunJobThread.start();

        toBeRingThread toBeRingThread = new toBeRingThread();
        toBeRingThread.setDaemon(true);
        toBeRingThread.setName("toBeRing");
        toBeRingThread.start();


        this.lock=routemanager.getDataLock();

    }

    //添加任务的入口，构造jobBean
    public void addJobController(String name,String pids,String className,String methodName,String paramType,String params,String cronExpr,Integer shardNum,boolean transfer,boolean reStart,String policy,String jobType){
        jobBean job;
        //是否是主动任务
        if(jobBean.java_normal.equals(jobType)||jobBean.shell_normal.equals(jobType)){
            job = new jobBean(new idUtil().nextId(),pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),getNextStartTime(cronExpr),jobBean.init,0,jobType);
        }
        else{
            job = new jobBean(new idUtil().nextId(),pids,className,methodName,paramType,params,name,cronExpr,shardNum,transfer,reStart,policy,new Date(),new Date(),0L,jobBean.waiting,0,jobType);
        }
        mapper.insertJob(job);
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
        }
        return true;
    }

    public returnMSG<List<jobBean>> getJobController(){
        List<jobBean> allJob = mapper.getAllJob();
        return new returnMSG<List<jobBean>>(200,"ok",allJob,allJob.size());
    }

    public void killJob(Long jobId){
        //todo 先判断是否需要kill 如果status为已经停用，就不再调用了
        try {
            lock.readLock().lockInterruptibly();
            jobBean jobById = mapper.getJobById(jobId);
            if(jobById.getStatus()==jobBean.stopped){
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.readLock().unlock();
        }

        List<executorLiveInfo> infos = this.routemanager.getAllExecutorInfo(jobId);
        //向所有jobId关联的executor 发送消息
        for (executorLiveInfo info : infos) {
            //生成future记录，根据requestId 获取对应结果
            defaultFuture future = new defaultFuture();
            Long requestId=idGenerator.nextLongId();
            this.futureMap.put(requestId,future);

            //发送消息
            this.server.sendMessage(new KillJobMessage(jobId,requestId),0,info.getChannel());

            //等待响应
            ResponseMessage msg = getFuture(requestId);
            if(msg==null){
                //超时 认为任务失败
                log.info("kill job fail");
            }else {
                int code = msg.getCode();
                log.info("Response code:"+code);
            }
        }
    }

    //启动前 5秒 读取任务
    private final long fetchTime=5 * 1000;

    private int testTime=4;

    //dependNormalTime以内完成的上游任务都认为有效
    private static final Long dependNormalTime=10* 60 *1000L;

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
    class toBeRunJobThread extends Thread{
        @Override
        public void run() {
            while (true){
                try {
                    TimeUnit.MILLISECONDS.sleep(fetchTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

                    log.info("jobList: "+jobList);
                    if(jobList==null||jobList.size()==0)
                        continue;
                    //先进行粗略的判断 假设 readTime 95 则list中包含0-100的所有任务
                    // 1. <=90 任务过期 根据错过重触发策略判断是否要触发
                    // 2. 90-95 任务可能因为调度问题而错过上一次的调度
                    // 3. >95 正常放入时间轮
                    for (jobBean job : jobList) {
                        //被动任务直接跳过 依赖任务先判断依赖任务是否全部完成
                        if(jobBean.java_passive.equals(job.getJobType())||jobBean.shell_passive.equals(job.getJobType())) {
                            continue;
                        }else if(job.getPids()!=null&&!isDependFinishNormal(job.getPids())){
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
                        job.setStatus(jobBean.doing);
                        mapper.update(job);
//                        mapper.updateJobInfoByStatusAndTimes(jobExecInfo.doing,jobExecInfo.Exec_Not_Use,job.getId());
                    }
                    log.info("更改完成");


                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    // commit
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            log.error(e.getMessage(), e);
                        }
                        try {
//                            conn.setAutoCommit(connAutoCommit);
                            conn.setAutoCommit(true);
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
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // item放着jobId
                List<Long> ringItemData = new ArrayList<>();
                int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                for (int i = 0; i < 2; i++) {
                    List<Long> tmpData = timeRing.remove( (nowSecond+60-i)%60 );
                    if (tmpData != null) {
                        ringItemData.addAll(tmpData);
                    }
                }

                if (ringItemData.size() > 0) {
                    // do trigger
                    log.info("job need to send");
                    for (Long jobId : ringItemData) {
                        sendJobToExecutor(mapper.getJobById(jobId));
                    }
                    // clear
                    ringItemData.clear();
                }
            }
        }
    }
//    //定时扫描jobTable
//    public void scanJobTable() throws ParseException {
//
//        //=====================================================
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//        if(testTime==4){
//            List<Long>list=new ArrayList<>();
//            list.add((long) 1);
//            list.add((long) 2);
////            addJobController("job1",null,"com.distribute.executor.service.helloService","hello", Arrays.asList("java.lang.String","java.lang.Integer"),Arrays.asList("dyw","18"),"* * * 8-31 * ?");
////            addJobController("job2",null,"com.distribute.executor.service.helloService","hello1", Arrays.asList("java.lang.String","java.lang.Integer"),Arrays.asList("dyw","18"),"* * * 8-31 * ?");
////            addJobController("job3",list,"com.distribute.executor.service.helloService","hello2", Arrays.asList("java.lang.String","java.lang.Integer"),Arrays.asList("dyw","18"),null);
//
//        }
//        testTime--;
//
//        //=====================================================
//        long currentTime=System.currentTimeMillis();
//        for (Map.Entry< Long,  jobExecInfo> next : this.jobTable.entrySet()) {
//            log.info("jobBeanInfos:"+next.getValue());
//            jobBean jojo = next.getValue().getJob();
//            long nextStartTime = jojo.getNextStartTime();
//            if(nextStartTime==0L){
//                //依赖任务 直接跳过
//                continue;
//            }
//            if(nextStartTime>currentTime&&nextStartTime-currentTime<fetchTime){
//                //到时间了，发送给executor
//                // 查询任务状态
//                // 1. 如果是正在执行，为了保持幂等性（同一个分片同一时间只能操作一个任务），不能执行
//                // 2. 如果是初始状态或者最终状态，就可以发送
//                jobExecInfo info = this.jobTable.get(jojo.getId());
//                if(info==null||info.getStatus()==jobExecInfo.init||info.getStatus()==jobExecInfo.finish){
//                    sendJobToExecutor(jojo);
//                }
//            }else if(nextStartTime<currentTime){
//                //过期任务，判断是否允许错过重触发
//                if(jojo.isRestart()){
//                    sendJobToExecutor(jojo);
//                }
//            }
//        }
//        log.info("scanJobTable ===== end");
//    }



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
                nameServerController.this.routemanager.sendJobToExecutor(jojo);
            }
        });
//        //更新jobtable
//        this.jobTable.get(jojo.getId()).setStatus(jobExecInfo.doing);
//        long nextTwoTime=0L;
//        //更新nextStartTime
//        if(!isRestart)
//            nextTwoTime=getNextTwoTime(jojo.getCronExpr());
//        else
//            nextTwoTime=getNextStartTime(jojo.getCronExpr());
//        jojo.setNextStartTime(nextTwoTime);
    }




    //todo 根据负载均衡策略 选择executor
//    public String getExecutor(){
//
//        return "executor-1";
//    }

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

    public void handleCallBackMessage(CallBackMessage msg) {
        int result = handleCallBack(msg);
    }

    //接受CallBackMessage，维护任务table
    //    1.如果code为200，任务完成，则设置isfinish=true
    //    2.         300, 任务出错，则认定为任务失败
    //    3.         400, executor断线，失效转移，让新的executor完成任务，由controller来做
    public int handleCallBack(CallBackMessage msg){
        String name = msg.getName();
        Long jobId = msg.getJobId();
        Integer index = msg.getShardIndex();
        Integer total = msg.getShardTotal();
        Integer code = msg.getCode();
        Integer execId=msg.getExecId();
        log.info("handleCallBack code:"+code+" jobId:"+jobId);
        try {
            lock.writeLock().lockInterruptibly();
            //修改状态 不需要修改executor_name
            mapper.updateJobDetail(jobId,execId,name,code,"");

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
                    log.info("jobId:"+jobId+" total finish!");
                }
            }else if(code==ResultEnum.error.result){
                log.info("jobId:"+jobId+" error!");
                //todo 任务失败，报警并记录

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.writeLock().unlock();
        }
        return code;
    }

//    //修改jobFinishDetail的状态
//    private void setStatus(List<jobFinishDetail> details,Long jobId,Integer code,boolean result){
//        for(jobFinishDetail detail:details){
//            if(detail.getJobId().equals(jobId)){
//                //找到对应的detail，修改code和状态
//                detail.setCode(code);
//                detail.setFinish(result);
//            }
//        }
//    }

    //检查是否全部完成
    private boolean isFinish(Long jobId,Integer execId){
        try {
            lock.readLock().lockInterruptibly();
            List<jobFinishDetail>details=mapper.getJobDetail(jobId,execId);
            if(details==null)return false;
            for (jobFinishDetail detail : details) {
                if(detail.getCode()!=ResultEnum.success.result){
                    log.info("isFinish:{} false!",jobId);
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
                    //为被动依赖任务 判断是否他的pid任务是否全部完成：
                    log.info("依赖任务："+job.getJobId()+" pids:"+job.getPids());
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

    public static void main(String[] args) throws InterruptedException, ParseException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

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


        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        reentrantReadWriteLock.readLock().lockInterruptibly();

        System.out.println("ff");
        reentrantReadWriteLock.readLock().unlock();

        reentrantReadWriteLock.writeLock().lockInterruptibly();
        System.out.println("gg");
        reentrantReadWriteLock.writeLock().lockInterruptibly();
        reentrantReadWriteLock.readLock().lockInterruptibly();

        System.out.println("ff");
        reentrantReadWriteLock.readLock().unlock();
        System.out.println("ggsdss");
        reentrantReadWriteLock.writeLock().unlock();

        reentrantReadWriteLock.writeLock().unlock();

    }

}

