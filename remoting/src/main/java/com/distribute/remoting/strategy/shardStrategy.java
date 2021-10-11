package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.executorInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class shardStrategy extends strategy{
    @Override
    public List<String> route(List<executorInfo> infos,Integer shardParam) {
        if(infos==null)return null;
        int num=infos.size();

        List<String>result=new ArrayList<>();
        if(num<=shardParam){
            for (executorInfo info : infos) {
                String name = info.getName();
                result.add(name);
            }
            return result;
        }else{
            //从num个机器中选择shardParam个机器出来分片完成任务
            //首先根据优先级来定，优先级高的先选;再根据已完成任务数，完成任务数少的先选
            Queue<compare>queue=new PriorityQueue<>();
            for (executorInfo info : infos) {
                queue.add(new compare(info));
            }
            int choose=0;
            while (choose<shardParam){
                compare poll = queue.poll();
                result.add(poll.i.getName());
                choose++;
            }
        }
        return result;
    }

//    public static void main(String[] args) {
//        List<executorInfo>infos=new ArrayList<>();
//        infos.add(new executorInfo("aaa","111",1,1));
//        infos.add(new executorInfo("b","111",1,2));
//        infos.add(new executorInfo("cc","111",3,5));
//        infos.add(new executorInfo("d","111",2,5));
//        infos.add(new executorInfo("e","111",2,5));
//
//        List<String> route = new shardStrategy().route(infos, 4);
//        System.out.println(route);
//
//    }
    static class compare implements Comparable<compare>{
        private executorInfo i;

        public compare(executorInfo i){
            this.i=i;
        }
        @Override
        public int compareTo(compare o) {
            if(i.getLevel()>o.i.getLevel()){
                return 1;
            }else if(i.getLevel()<o.i.getLevel()){
                return -1;
            }else {
                if(i.getCount()<=o.i.getCount()){
                    return -1;
                }else {
                    return 1;
                }
            }
        }
    }
}
