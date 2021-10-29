package com.distribute.executor.Message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallBackMessage extends Message{
    public CallBackMessage(String name, Integer code, Integer shardIndex, Integer shardTotal, Long jobId,Long requestId,Integer execId) {
        this.name = name;
        this.code = code;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.jobId = jobId;
        this.setRequestId(requestId);
        this.execId=execId;
    }

    @Override
    public int getMessageType() {
        return CallBackMessage;
    }

    private String name;
    private Integer code;//200 ok 500 GG
    private Integer shardIndex;//是作为第几个分片完成的job
    private Integer shardTotal;
    private Long jobId;
    private Integer execId;
    private byte[] content;
    private boolean isCompress;
//    private Integer jobType; //todo 暂不支持多种类型的job
}
