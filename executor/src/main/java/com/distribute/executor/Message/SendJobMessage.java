package com.distribute.executor.Message;

import com.distribute.executor.bean.jobBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendJobMessage extends Message{
    public SendJobMessage(jobBean job, Integer shardIndex, Integer shardTotal,Long requestId,Integer execId) {
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

    private jobBean job;

    private Integer shardIndex;

    private Integer shardTotal;

    private Integer execId;

}