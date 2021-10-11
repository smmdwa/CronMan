package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.executorInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class randomStrategy extends strategy {
    @Override
    public List<String> route(List<executorInfo> infos, Integer shardParam) {
        if(infos==null||infos.size()<shardParam)return null;
        //随机取shardParam个数
        HashSet<Integer>set=new HashSet<>();
        int time=0;
        while (set.size()<shardParam){
            set.add(new Random().nextInt(infos.size()));
            time++;
            //防止死循环，即infos的总可用个数小于要求的分片个数
            if(time>=20){
                return null;
            }
        }
        List<String> res=new ArrayList<>();
        for (Integer integer : set) {
            res.add(infos.get(integer).getName());
        }
        return res;
    }
}
