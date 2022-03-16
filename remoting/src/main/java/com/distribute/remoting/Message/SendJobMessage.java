package com.distribute.remoting.Message;

import com.distribute.remoting.bean.JobBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendJobMessage extends Message{
    public SendJobMessage(JobBean job, Integer shardIndex, Integer shardTotal, Long requestId, Integer execId) {
        this.job = job;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.setRequestId(requestId);
        this.execId=execId;
    }

    @Override
    public int getMessageType() {
        return SendJobMessage;
    }

    private JobBean job;

    private Integer shardIndex;

    private Integer shardTotal;

    private Integer execId;

    private byte[][] contents;

    private boolean[] isCompresses;

}