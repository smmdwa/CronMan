package com.distribute.remoting.DAG;

import com.distribute.remoting.DAG.NodeDAG;
import lombok.Data;

import java.util.*;

//十字链表
@Data
public class Dag {
    private int mavVex;
    private VertexType[] verArr;

    // 顶点数据
    @Data
    private static class VertexType {
        NodeDAG node;
        AdjacencyType firstIn = null;// 入边表（顶点为弧尾）
        AdjacencyType firstOut = null;// 出边表（顶点为弧头）

        public VertexType(NodeDAG node) {
            this.node = node;
        }
    }
    // 顶点数组赋值
    public Dag(List<NodeDAG> nodes) {
        this.mavVex = nodes.size();
        this.verArr = new VertexType[this.mavVex];
        for (int i = 0; i < nodes.size(); i++) {
            this.verArr[i] = new VertexType(nodes.get(i));
        }
    }

    public void getRelatedDAG(Long jobId) {
        int position = getPosition(jobId);
        this.result = new HashSet<>();
        getDAG(position);
        fillNodesLevel();
    }

    HashSet<Integer> result;

    @Data
    private static class AdjacencyType {
        int from = -1;// 弧的起点在顶点数组中的下标
        int to = -1;// 弧终点在顶点数组中的下标
        AdjacencyType headLink = null;// 指向下一个终点相同的邻接点
        AdjacencyType adjLink = null;// 指向下一个起点相同的邻接点

    }

    public HashSet<NodeDAG> transferToNodeDag() {
        HashSet<NodeDAG> res = new HashSet<>();
        for (Integer integer : result) {
            res.add(verArr[integer].getNode());
        }
        return res;
    }

    public void getDAG(int pos) {
        List<Integer> toBeSearch = new ArrayList<>();
        result.add(pos);
        AdjacencyType fristin = this.verArr[pos].getFirstIn();
        AdjacencyType fristout = this.verArr[pos].getFirstOut();
        while (fristin != null) {
            if (!result.contains(fristin.getFrom())) {
                toBeSearch.add(fristin.getFrom());
            }
            fristin = fristin.getHeadLink();
        }
        while (fristout != null) {
            if (!result.contains(fristout.getTo())) {
                toBeSearch.add(fristout.getTo());
            }
            fristout = fristout.getAdjLink();
        }

        for (Integer beSearch : toBeSearch) {
            getDAG(beSearch);
        }
    }

    public void addAdj(Long cur, Long next) {
        AdjacencyType temp = null;
        AdjacencyType tempin = null;
        int posOne = getPosition(cur);
        int posTwo = getPosition(next);
        AdjacencyType adj = new AdjacencyType();
        adj.setFrom(posOne);
        adj.setTo(posTwo);
        temp = this.verArr[posOne].getFirstOut();
        tempin = this.verArr[posTwo].getFirstIn();
        if (temp == null) {
            this.verArr[posOne].setFirstOut(adj);
        } else {
            while (true) {
                if (temp.getAdjLink() != null) {
                    temp = temp.getAdjLink();
                } else {
                    temp.setAdjLink(adj);
                    break;
                }
            }
        }
        if (tempin == null) {
            this.verArr[posTwo].setFirstIn(adj);
        } else {
            while (true) {
                if (tempin.getHeadLink() != null) {
                    tempin = tempin.getHeadLink();
                } else {
                    tempin.setHeadLink(adj);
                    break;
                }
            }
        }

    }

    private int getPosition(Long jobId) {
        int pos = 0;
        for (int i = 0; i < this.mavVex; i++) {
            if (this.verArr[i].getNode().getJobId().equals(jobId)) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    //填充node大致的level和belong，为什么是大致，因为DAG不是层级结构，为了前端展示，所以制造大致的层级
    //找到所有入度为0的节点，
    private void fillNodesLevel() {
        int belong = 1;
        for (Integer integer : this.result) {
            NodeDAG node = verArr[integer].getNode();
            if (verArr[integer].getFirstIn() != null)
                continue;
            //找到入度为0的节点开始
            fill(integer, 1, belong);
            belong++;
        }
    }

    //进行实际的填充
    private void fill(Integer pos, int level, int belong) {
        NodeDAG node = verArr[pos].getNode();
        //已经被设置了就跳过
        if (node.getLevel() != 0) return;
        node.setLevel(level);
        node.setBelong(belong);
        AdjacencyType adjLink = verArr[pos].getFirstOut();
        while (adjLink != null) {
            fill(adjLink.getTo(), level + 1, belong);
            adjLink = adjLink.getAdjLink();
        }
    }

    //清除所有的level和belong
    private void clear() {
        for (Integer integer : result) {
            verArr[integer].getNode().setLevel(0);
            verArr[integer].getNode().setBelong(0);
        }
    }

}