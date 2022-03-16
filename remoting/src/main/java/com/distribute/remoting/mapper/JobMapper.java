package com.distribute.remoting.mapper;

import com.distribute.remoting.bean.JobBean;
import com.distribute.remoting.bean.JobExecInfo;
import com.distribute.remoting.bean.JobFinishDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@Mapper
public interface JobMapper {

    //========================= jobBean

    JobBean getJobById(long jobId);

    List<JobBean> getAllJob();

    int insertJob(JobBean job);

    int update(JobBean job);

    int updateJobStatusById(long jobId,int status);

    int updateJobFinish(long jobId, int status, Date updateTime);

    int updateJobStatusAndTimeById(long jobId,int status);

    List<JobBean> getToBeRunJob(@Param("nextStartTime") long nextStartTime);

    int updateJobDisable(long jobId);

    //========================= jobExecInfo

    List<JobBean> getAllJobInfo();

    int insertJobInfo(JobExecInfo info);

    int updateJobInfoByStatusAndTimes(@Param("status") int status,@Param("exec_times") int exec_times,@Param("jobId") long jobId);

    JobExecInfo getJobInfoById(long id);


    //=======================  jobDetail

    //断联 寻找对应执行器执行的任务，并且code为初始值
    List<JobFinishDetail> getNotExecJobs(String name);

    //killJob 根据jobId寻找所有相关联的execName execTimes相同
    List<String> getExecName(long jobId);

    List<JobFinishDetail> getJobDetail(long jobId, int execId);

    int insertJobDetail(JobFinishDetail job);

    int updateJobDetail(long jobId,int execId,String executorName,int code,String newExecutorName,byte[]content,boolean isCompress);

}
