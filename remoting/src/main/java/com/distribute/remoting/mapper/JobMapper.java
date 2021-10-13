package com.distribute.remoting.mapper;

import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.bean.jobExecInfo;
import com.distribute.remoting.bean.jobFinishDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@Mapper
public interface JobMapper {

    //========================= jobBean

    jobBean getJobById(long jobId);

    List<jobBean> getAllJob();

    int insertJob(jobBean job);

    int update(jobBean job);

    int updateJobStatusById(long jobId,int status);

    int updateJobFinish(long jobId, int status, Date updateTime);

    int updateJobStatusAndTimeById(long jobId,int status);

    List<jobBean> getToBeRunJob(@Param("nextStartTime") long nextStartTime);

    int updateJobDisable(long jobId);

    //========================= jobExecInfo

    List<jobBean> getAllJobInfo();

    int insertJobInfo(jobExecInfo info);

    int updateJobInfoByStatusAndTimes(@Param("status") int status,@Param("exec_times") int exec_times,@Param("jobId") long jobId);

    jobExecInfo getJobInfoById(long id);


    //=======================  jobDetail

    //断联 寻找对应执行器执行的任务，并且code为初始值
    List<jobFinishDetail> getNotExecJobs(String name);

    //killJob 根据jobId寻找所有相关联的execName execTimes相同
    List<String> getExecName(long jobId);

    List<jobFinishDetail> getJobDetail(long jobId,int execId);

    int insertJobDetail(jobFinishDetail job);

    int updateJobDetail(long jobId,int execId,String executorName,int code,String newExecutorName);

}
