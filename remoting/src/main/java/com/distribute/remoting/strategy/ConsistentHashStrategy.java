package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.ExecutorInfo;
import com.distribute.remoting.utils.Strategy.ConsistentHash;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class ConsistentHashStrategy extends Strategy {
    @Override
    public List<String> route(List<ExecutorInfo> infos, Integer shardParam) {
        if(infos==null)return null;
        ConsistentHash ch=ConsistentHash.getInstance();
//        List<String> nodes=new ArrayList<>();
        List<String> res=new ArrayList<>();
//        for(ExecutorInfo info:infos)nodes.add(info.getName());
        ch.buildHashCircle(infos);
        for (int i = 0; i < shardParam; i++) {
            String node = ch.findNodeByKey("key:" + System.currentTimeMillis()+i);
            res.add(node);
        }
        return res;
    }

    public static void main(String[] args) {
        ConsistentHashStrategy chs=new ConsistentHashStrategy();
        List<ExecutorInfo>infoList=new ArrayList<>();
        infoList.add(new ExecutorInfo("1","",1,1));
        infoList.add(new ExecutorInfo("2","",1,2));
        infoList.add(new ExecutorInfo("3","",1,1));
        infoList.add(new ExecutorInfo("4","",1,1));
        infoList.add(new ExecutorInfo("5","",1,5));

        List<String> route = chs.route(infoList, 3);
        System.out.println(route);

        System.out.println(chs.route(infoList, 2));
        System.out.println(chs.route(infoList, 4));
        System.out.println(chs.route(infoList, 1));
    }
}
