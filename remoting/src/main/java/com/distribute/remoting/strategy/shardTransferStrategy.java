package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.ExecutorInfo;

import java.util.List;

public class shardTransferStrategy extends Strategy {
    @Override
    public List<String> route(List<ExecutorInfo> infos, Integer shardParam) {

        return null;
    }
}
