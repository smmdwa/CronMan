package com.distribute.executor.utils;

import com.distribute.executor.Message.ResponseMessage;
import com.distribute.executor.response.defaultFuture;

import java.util.Map;

public class FutureUtil {
    public static ResponseMessage getFuture(Map<Long, defaultFuture> futureMap, Long requestId) {
        try {
            defaultFuture future = futureMap.get(requestId);
            return future.getResponse(2000); //2秒就算超时
        } finally {
            //获取成功以后，从map中移除
            futureMap.remove(requestId);
        }
    }

    public static boolean setFuture(Map<Long, defaultFuture> futureMap,ResponseMessage msg) {
        defaultFuture future = futureMap.get(msg.getRequestId());
        if(future==null){
            return false;
        }
        future.setResponse(msg);
        return true;
    }
}
