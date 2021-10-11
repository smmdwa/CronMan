package com.distribute.remoting.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseMessage extends Message{

    private int code;  //200success  500error

    private String msg;

    public static final int success=200;

    public static final int error=500;

    public ResponseMessage(Long requestId,int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.setRequestId(requestId);
    }

    //content?

    @Override
    public int getMessageType() {
        return ResponseMessage;
    }

}
