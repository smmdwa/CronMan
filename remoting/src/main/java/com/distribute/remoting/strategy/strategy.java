package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.executorInfo;
import com.distribute.remoting.bean.executorLiveInfo;

import java.util.List;

public abstract class strategy {

    public abstract List<String> route(List<executorInfo> infos,Integer shardParam);
}
