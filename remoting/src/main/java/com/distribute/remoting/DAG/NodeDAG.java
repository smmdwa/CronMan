package com.distribute.remoting.DAG;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeDAG {

    // a---b---c
    // c---|   |
    // d-------|
    //1.入度为0的节点为第一层级，此后依赖于它的 层级递增，但是不是绝对的层级，只是用于确定x轴
    //从1开始
    int level;

    //属于哪一队列，同上也不是绝对的，只用于确定前端的y轴
    int belong;

    String name;

    Long jobId;

    int status;

    public NodeDAG(String name,Long jobId){
        this.name=name;
        this.jobId=jobId;
        level=0;
        belong=0;
        status=0;
    }

}
