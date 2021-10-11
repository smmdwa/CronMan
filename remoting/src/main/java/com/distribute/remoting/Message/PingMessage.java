package com.distribute.remoting.Message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PingMessage extends Message {
    private String name;

    public PingMessage(String name,Long requestId) {
        this.name = name;
        this.setRequestId(requestId);
    }

    @Override
    public int getMessageType() {
        return PingMessage;
    }
}
