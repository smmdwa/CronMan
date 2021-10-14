package com.distribute.remoting.netty_server;


import com.distribute.remoting.Message.CallBackMessage;
import com.distribute.remoting.Message.SendJobMessage;
import com.distribute.remoting.bean.*;
import com.distribute.remoting.mapper.JobMapper;
import com.distribute.remoting.strategy.strategy;
import com.distribute.remoting.strategy.strategyEnum;
import com.distribute.remoting.thread.sendJobThread;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@AllArgsConstructor
@Slf4j
@Component
public class routeInfoManager {

    private final ReadWriteLock executorLock = new ReentrantReadWriteLock();

    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

    private final HashMap<String, executorInfo> executorAddrTable;
    private final HashMap<String, executorLiveInfo> executorLiveTable;
//    private final ConcurrentHashMap<Long, List<jobFinishDetail>>jobFinishTable;//任务完成情况表 jobId对应不同的detail

    private volatile static routeInfoManager instance;

    private LinkedBlockingQueue<jobFinishDetail> jobToBeTransferQueue;

    @Autowired
    JobMapper mapper;
//    //todo 顺序问题？
//    public static routeInfoManager getInstance() {
//        if (instance == null) {
//            synchronized (routeInfoManager.class) {
//                if (instance == null) {
//                    instance = new routeInfoManager();
//                }
//            }
//        }
//        return instance;
//    }

    public routeInfoManager() {
        this.executorAddrTable = new HashMap<String, executorInfo>(128);
        this.executorLiveTable = new HashMap<String, executorLiveInfo>(128);
        this.jobToBeTransferQueue=new LinkedBlockingQueue<>();
    }

    //注册新来的执行器
    public boolean registerExecutor(String name,String addr,Channel channel,Integer level){

        try {
            this.executorLock.writeLock().lockInterruptibly();

            executorInfo executorInfo = new executorInfo(name,addr,0,level);
            executorLiveInfo executorLiveInfo = new executorLiveInfo(System.currentTimeMillis(), channel, addr);

            this.executorAddrTable.put(name,executorInfo);
            this.executorLiveTable.put(name,executorLiveInfo);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.executorLock.writeLock().unlock();
        }
        log.info("routeInfoManager ===== registerExecutor end");
        log.info("table:"+this.executorAddrTable);
        log.info("aliveTable:"+this.executorLiveTable);
        return true;
    }

    //接受心跳包，更新
    public boolean updateActiveExecutor(String name){

        try {
            this.executorLock.writeLock().lockInterruptibly();
            this.executorLiveTable.get(name).setLastUpdateTimestamp(System.currentTimeMillis());

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            this.executorLock.writeLock().unlock();
        }
//        log.info("routeInfoManager ===== updateActiveExecutor end");
        log.info("table:"+this.executorAddrTable);
//        log.info("aliveTable:"+this.executorLiveTable);
        return true;
    }


//    @Value("${controller.channel.expiredTime:30000}")
    private long executorChannelExpiredTime=30000; // 30秒就认定为离线

    // 扫描存活的executorLiveInfo  拿出brokerLiveTable的executorLiveInfo 比较
    public void scanNotActiveExecutor() {
        try {
            this.executorLock.readLock().lockInterruptibly();
            for (Map.Entry<String, executorLiveInfo> next : this.executorLiveTable.entrySet()) {
                long last = next.getValue().getLastUpdateTimestamp();
                //本地 brokerLiveTable 默认2分钟过期
                //todo  System.currentTimeMillis()   不同executor  可能存在误差？
                if ((last + executorChannelExpiredTime) < System.currentTimeMillis()) {
                    //需要移除

                    //1. 关闭channel
                    next.getValue().getChannel().close();

                    //2. 删除其余关联数据
                    destory(next.getKey());

                    //0. 标记它所关联的未完成的任务，修改code为400  并进行失效转移
                    changeCode(next.getKey());

                    log.info("scanNotActiveExecutor remove");
                }

            }
//            log.info("routeInfoManager ====== scanNotActiveBroker end");
            log.info("table:"+this.executorAddrTable);
//            log.info("aliveTable:"+this.executorLiveTable);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.executorLock.readLock().unlock();
        }
    }

    //根据channel获取ExecutorName
    public String getExecutorName(Channel channel){
        try {
            this.executorLock.readLock().lockInterruptibly();
            for (Map.Entry<String, executorLiveInfo> infos : this.executorLiveTable.entrySet()) {
                if(infos.getValue().getChannel()==channel){
                    return infos.getKey();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.executorLock.readLock().unlock();
        }
        return null;
    }

    public Channel getExecutorChannel(String name) {
        try {
            this.executorLock.readLock().lockInterruptibly();
            return this.executorLiveTable.get(name).getChannel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }finally {
            this.executorLock.readLock().unlock();
        }
    }

        //从table中 选择一个可用的executor 拿到channel 发送SendJobMessage
    public void sendJobToExecutor(jobBean job){
        try {
            this.executorLock.readLock().lockInterruptibly();
            log.info("sendJob:"+job);
            Integer shardParam=job.getShardNum();
            strategy strategy = strategyEnum.match(job.getPolicy()).getStrategy();
            //获取所有的executorInfo
            List<executorInfo> infos = new ArrayList<>(this.executorAddrTable.values());
            //根据策略获取可用的executorName
            List<String> sendList=strategy.route(infos,shardParam);
            if(sendList==null||sendList.size()==0){
                //todo 暂时没有可用的executor
                // 将任务持久化在数据库，设置状态为 not started 定时任务扫描数据库，拿到没开始的任务重新跑
                log.info("no executor");
                return;
            }
            //根据executorName获取要发送的executorLiveInfos
            List<executorLiveInfo> liveInfos=new ArrayList<>();
            for(String sendExecutor:sendList){
                executorLiveInfo liveInfo = this.executorLiveTable.get(sendExecutor);
                if(liveInfo!=null){
                    liveInfos.add(liveInfo);
                }
            }
            int index=0,total=liveInfos.size();
            log.info("liveInfos:"+liveInfos);
//            List<jobFinishDetail>details=new ArrayList<>();
            //再上一把锁 锁住的是竞争修改表的
            try {
                this.dataLock.writeLock().lockInterruptibly();
                for(executorLiveInfo liveInfo:liveInfos){
                    Channel channel = liveInfo.getChannel();
                    int finalIndex = index;
                    //丢给sendJob来完成任务
                    sendJobThread.pushSendJob(new jobSendDetail(job.getJobId(),job,channel,finalIndex,total,job.getExecTimes()));
                    index++;
                    //记录进jobDetail
                    jobFinishDetail detail = new jobFinishDetail(job.getJobId(), job.getExecTimes(), job, ResultEnum.init.result, getExecutorName(channel), finalIndex, total);
                    log.info("detail:"+detail);
                    mapper.insertJobDetail(detail);
                }
            }finally {
                this.dataLock.writeLock().unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.executorLock.readLock().unlock();
        }
        log.info("sendJobToExecutor:"+job.getJobId());
    }

    //失效转移
    public void transferToExecutor(jobBean job,jobFinishDetail detail) {
        //如果job不允许失效转移，那就return
        if(!job.isTransfer())return;
        //读executortable 需要上锁
        try {
            executorLock.readLock().lockInterruptibly();
            strategy strategy = strategyEnum.match("random").getStrategy();
            //获取所有的executorInfo
            List<executorInfo> infos = new ArrayList<>(this.executorAddrTable.values());
            //todo 如果全都挂掉了怎么办？  初步方案，后续添加 扫描线程，监控jobtable，遇到code400 再次调用本函数
            //根据策略获取可用的executorName
            List<String> sendList=strategy.route(infos,1);
            if(sendList==null||sendList.size()==0){
                //todo 暂时没有可用的executor
                // 将任务持久化在数据库，设置状态为 not started 定时任务扫描数据库，拿到没开始的任务重新跑
                log.info("no executor");
                return;
            }
            //根据executorName获取要发送的executorLiveInfos
            executorLiveInfo info = this.executorLiveTable.get(sendList.get(0));
            log.info("transfer choose:"+info.getChannel());
            //发送
            sendJobThread.pushSendJob(new jobSendDetail(detail.getJobId(),job,info.getChannel(),detail.getShardIndex(),detail.getShardTotal(),detail.getExecId()));
            //更新jobDetail 要修改execName
            mapper.updateJobDetail(detail.getJobId(),detail.getExecId(),detail.getExecutorName(),ResultEnum.change.result,infos.get(0).getName());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            executorLock.readLock().unlock();
        }
    }

    @Resource
    DataSource dataSource;

    //executor断联 修改code400 todo 修改成排他锁，通过修改detail.getCode()  来使其他调度器进不去失效转移
    public void changeCode(String name){
        //搜寻所有的jobFinishTable
        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = dataSource.getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            preparedStatement = conn.prepareStatement(  "select * from jobLock where mylock = 'lock' for update" );

            preparedStatement.execute();

            List<jobFinishDetail> notExecJobs = mapper.getNotExecJobs(name);
            for(jobFinishDetail detail:notExecJobs){
                if (detail.getExecutorName().equals(name) && detail.getCode() == ResultEnum.init.result) {
                    // 修改code
//                    mapper.updateJobDetail(detail.getJobId(),detail.getExecId(),ResultEnum.change.result);
//                    detail.setCode(ResultEnum.change.result);
                    // 失效转移  选择新的executor发送  分片为1
                    transferToExecutor(detail.getJob(),detail);
                }
            }

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


//        for (Map.Entry<Long, List<jobFinishDetail>> next : this.jobFinishTable.entrySet()) {
//            for (jobFinishDetail detail : next.getValue()) {
//                if (detail.getName().equals(name) && detail.getCode() == ResultEnum.init.result) {
//                    // 修改code
//                    detail.setCode(ResultEnum.change.result);
//                    // 失效转移  选择新的executor发送  分片为1
//                    transferToExecutor(detail.getJobId(),detail.getJob(),detail.getShardIndex(),detail.getShardTotal());
//                }
//            }
//        }
//        log.info("table:"+this.jobFinishTable);
//        log.info("changeCode:"+name);

    }

    //删除断联的executor在table中的信息
    public void destory(String name){
        try{
            this.executorLock.writeLock().lockInterruptibly();
            //删除缓存数据
            this.executorAddrTable.remove(name);
            this.executorLiveTable.remove(name);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            this.executorLock.writeLock().unlock();
        }
        log.info("destory end:"+name);

    }



    //根据jobId获取所有关联的executor路由信息
    public List<executorLiveInfo> getAllExecutorInfo(Long jobId) {
        List<String> execName = mapper.getExecName(jobId);
        List<executorLiveInfo> infos =new ArrayList<>();
        try {
            this.executorLock.readLock().lockInterruptibly();
            for (String name : execName) {
                executorLiveInfo info = this.executorLiveTable.get(name);
                if(info!=null)
                    infos.add(info);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }finally {
            this.executorLock.readLock().unlock();
        }
        return infos;
    }



}
