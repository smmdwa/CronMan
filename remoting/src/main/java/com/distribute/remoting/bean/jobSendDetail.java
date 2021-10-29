package com.distribute.remoting.bean;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;



@Data
@AllArgsConstructor
public class jobSendDetail {
    //将要发送的job信息
    private Long jobId;

    private jobBean job;

    private Channel channel;

    private Integer index;

    private Integer total;

    private Integer execId;

    private byte[][] contents;

    private boolean[] isCompresses;


}
