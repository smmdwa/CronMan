package com.distribute.remoting.bean;


import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class executorLiveInfo {
    private long lastUpdateTimestamp;
//    private DataVersion dataVersion;
    private Channel channel;

    private String executorAddr;

}
