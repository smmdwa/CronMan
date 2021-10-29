package com.distribute.remoting.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class jobFinishDetail {

    private Long jobId;

    //任务执行的id，第几次执行，初始化为0
    private Integer execId;

    private jobBean job;

    private Integer code;//0初始 200完成 300任务出错 400对应的executor断线

    private String executorName;//归属于哪个executor的任务

    private Integer shardIndex;//分片index

    private Integer shardTotal;

    private byte[] content;//任务完成后的消息，父亲任务提供给子任务

    private Boolean isCompress;//是否被压缩


}
