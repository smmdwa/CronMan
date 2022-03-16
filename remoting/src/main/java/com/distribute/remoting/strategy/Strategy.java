package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.ExecutorInfo;

import java.util.List;

public abstract class Strategy {

    public abstract List<String> route(List<ExecutorInfo> infos, Integer shardParam);
}
