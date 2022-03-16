package com.distribute.remoting.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KillJobMessage extends Message{
    public KillJobMessage(Long jobId,Long requestId,String sourceAddr,String targetAddr) {
        this.jobId = jobId;
        this.setRequestId(requestId);
        this.setSourceAddr(sourceAddr);
        this.setTargetAddr(targetAddr);
    }

    @Override
    public int getMessageType() {
        return KillJobMessage;
    }

    private Long jobId;

    private String sourceAddr;

    private String targetAddr;


}