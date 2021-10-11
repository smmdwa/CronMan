package com.distribute.remoting.Message;

import com.distribute.remoting.bean.jobBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KillJobMessage extends Message{
    public KillJobMessage(Long jobId,Long requestId) {
        this.jobId = jobId;
        this.setRequestId(requestId);
    }

    @Override
    public int getMessageType() {
        return KillJobMessage;
    }

    private Long jobId;


}