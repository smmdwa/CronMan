package com.distribute.remoting.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class jobBean implements Serializable {

    private long jobId;

    private String pids;

    private String className;

    private String methodName;

    private String parameterTypes;

    private String args;

    private String name;

    private String cronExpr;

    private Integer shardNum;

    //是否支持失效转移
    private boolean transfer;

    //是否支持任务重触发
    private boolean restart;

    //调度策略
    private String policy;

    private Date createTime ;

    //任务上次结束的时间
    private Date updateTime ;

    private long nextStartTime;

    /**
     * 状态，0表示已就绪，1表示执行中，2表示停用  3表示等待上游任务执行
     */
    public static final int init=0;
    public static final int doing=1;
    public static final int stopped=2;
    public static final int waiting=3;
    //执行次数
    public static final int Exec_Not_Use=-1;
    private int status;

    private int execTimes;

    public static final String java_passive="java_passive";
    public static final String java_normal="java_normal";
    public static final String shell_passive="shell_passive";
    public static final String shell_normal="shell_normal";
    //任务类型 暂时有四种 java被动 shell被动 java普通任务 shell普通任务
    private String jobType;
}
