package com.distribute.remoting.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//任务执行情况，监控任务完成状态，
@Data
@AllArgsConstructor
@NoArgsConstructor
public class jobExecInfo {

    private String name;

    private Long jobId;

    private jobBean job;

    /**
     * 状态，0表示未开始，1表示执行中，2表示已完成  3表示等待依赖任务执行
     */
    private int status;

    public static final int init=0;
    public static final int doing=1;
    public static final int finish=2;
    public static final int waiting=3;
    //执行次数
    public static final int Exec_Not_Use=-1;
    private int execTimes ;

}
