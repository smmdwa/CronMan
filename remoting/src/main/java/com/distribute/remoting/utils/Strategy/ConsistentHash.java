package com.distribute.remoting.utils.Strategy;

import com.distribute.remoting.bean.ExecutorInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class ConsistentHash {
    private int virtualNum = 5;  //平均虚拟节点数

    private HashAlgorithm alg = HashAlgorithm.KETAMA_HASH;//采用的HASH算法

    private Set<String> nodeSet;  //节点列表

    private final TreeMap<Long/* 节点hash */, String/* 节点 */> nodeMap = new TreeMap<Long, String>();

    private static class SingletonHolder {
        private static ConsistentHash instance = new ConsistentHash();
    }

    private ConsistentHash() {
    }

    public static ConsistentHash getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 构建一致性HASH环
     */
    public void buildHashCircle(List<ExecutorInfo> nodes) {
        if(nodes==null)return;
        for(ExecutorInfo info:nodes){
            int virtualNum=info.getLevel();
            if(virtualNum<1)virtualNum=1;
            String node=info.getName();
            for (int i = 0; i < virtualNum; i++) {
                long nodeKey = this.alg.hash(node+ "-" + i);
                nodeMap.put(nodeKey, node);
            }
        }
//        HashSet<String>set=new HashSet<>(nodes);
//        this.setNodeList(set);
//        if (nodeSet == null) return;
//        for (String node : nodeSet) {
//            for (int i = 0; i < virtualNum; i++) {
//                long nodeKey = this.alg.hash(node+ "-" + i);
//                nodeMap.put(nodeKey, node);
//            }
//        }
    }

    /**
     * 沿环的顺时针找到虚拟节点
     *
     * @param key
     * @return
     */
    public String findNodeByKey(String key) {
        final Long hash = this.alg.hash(key);
        Long target = hash;
        if (!nodeMap.containsKey(hash)) {
            target = nodeMap.ceilingKey(hash);
            if (target == null && !nodeMap.isEmpty()) {
                target = nodeMap.firstKey();
            }
        }
        return nodeMap.get(target);
    }

    /**
     * 设置每个节点的虚拟节点个数，该参数默认是100
     *
     * @param virtualNum 虚拟节点数
     */
    public void setVirtualNum(int virtualNum) {
        this.virtualNum = virtualNum;
    }

    /**
     * 设置一致性HASH的算法，默认采用 KETAMA_HASH
     * 对于一致性HASH而言选择的HASH算法首先要考虑发散度其次再考虑性能
     *
     * @param alg 具体支持的算法
     * @see HashAlgorithm
     */
    public void setAlg(HashAlgorithm alg) {
        this.alg = alg;
    }

    /**
     * 配置实际的节点，允许同一个IP上多个节点，但是应该用name区分开
     *
     * @param nodeList 节点列表
     */
    public void setNodeList(Set<String> nodeList) {
        this.nodeSet = nodeList;
    }

    /**
     * 获取环形HASH
     *
     * @return
     */
    public TreeMap<Long, String> getNodeMap() {
        return nodeMap;
    }
}
