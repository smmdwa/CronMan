package com.distribute.executor.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterInMessage extends Message{
    public RegisterInMessage(String addr, String name, Integer level,Long requestId) {
        this.addr = addr;
        this.name = name;
        this.level = level;
        this.setRequestId(requestId);
    }

    @Override
    public int getMessageType() {
        return RegisterInMessage;
    }

    private String addr;
    private String name;
    private Integer level;

}
