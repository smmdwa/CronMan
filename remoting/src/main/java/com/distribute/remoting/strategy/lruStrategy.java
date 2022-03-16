package com.distribute.remoting.strategy;

import com.distribute.remoting.bean.ExecutorInfo;

import java.util.*;

//最近最少使用的执行器，优先被调用
public class lruStrategy extends Strategy {
    private static LRUCache lruCache=new LRUCache();

    @Override
    public List<String> route(List<ExecutorInfo> infos, Integer shardParam) {
        if(infos==null||infos.size()<shardParam)return null;

        // put new
        for (ExecutorInfo info: infos) {
            if (!lruCache.contains(info)) {
                lruCache.put(info);
            }
        }
        // remove old
        List<ExecutorInfo> all = lruCache.getAll();
        for (ExecutorInfo info : all) {
            boolean exist=false;
            for (ExecutorInfo nowInfo : infos) {
                if(nowInfo.getName().equals(info.getName())){
                    exist=true;
                }
            }
            if(!exist){
                lruCache.remove(info);
            }
        }
        List<String>res=new ArrayList<>();
        int num=0;
        while (num<shardParam){
            ExecutorInfo executorInfo = lruCache.get();
            res.add(executorInfo.getName());
            num++;
        }
        return res;
    }

}
 class LRUCache {
    class DLinkedNode {
        ExecutorInfo value;
        DLinkedNode prev;
        DLinkedNode next;
        public DLinkedNode() {}
        public DLinkedNode(ExecutorInfo _value) { value = _value;}
    }

    private DLinkedNode head, tail;

    public LRUCache() {
        // 使用伪头部和伪尾部节点
        head = new DLinkedNode();
        tail = new DLinkedNode();
        head.next = tail;
        tail.prev = head;
    }

    //获取最后一个节点，再把它移到头部
    public ExecutorInfo get() {
        DLinkedNode tail = getTail();
        moveToHead(tail);
        return tail.value;
    }

    public void put(ExecutorInfo value) {
        DLinkedNode newNode = new DLinkedNode(value);
        addToHead(newNode);
    }

    public boolean contains(ExecutorInfo value){
        DLinkedNode headNode=head.next;
        while (headNode!=tail){
            if(headNode.value.getName().equals(value.getName())){
                return true;
            }
            headNode=headNode.next;
        }
        return false;
    }

    public List<ExecutorInfo> getAll(){
        List<ExecutorInfo> list=new ArrayList<>();
        DLinkedNode headNode=head.next;
        while (headNode!=tail){
            list.add(headNode.value);
            headNode=headNode.next;
        }
        return list;
    }

     public void remove(ExecutorInfo value) {
         DLinkedNode headNode=head.next;
         while (headNode!=tail){
             if(headNode.value.equals(value)){
                 break;
             }
             headNode=headNode.next;
         }
         if(headNode==tail)return;
         DLinkedNode node=headNode;
         node.prev.next = node.next;
         node.next.prev = node.prev;
     }
     public void remove(DLinkedNode node) {
         node.prev.next = node.next;
         node.next.prev = node.prev;
     }

    private void addToHead(DLinkedNode node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void moveToHead(DLinkedNode node) {
        remove(node);
        addToHead(node);
    }

    private DLinkedNode removeTail() {
        DLinkedNode res = tail.prev;
        remove(res);
        return res;
    }

    private DLinkedNode getTail(){
        return tail.prev;
    }

     public static void main(String[] args) {
         lruStrategy lruStrategy = new lruStrategy();
         List<ExecutorInfo>infoList=new ArrayList<>();
         infoList.add(new ExecutorInfo("1","",1,1));
         infoList.add(new ExecutorInfo("2","",1,1));
         infoList.add(new ExecutorInfo("3","",1,1));
         infoList.add(new ExecutorInfo("4","",1,1));
         infoList.add(new ExecutorInfo("5","",1,1));

         List<String> route = lruStrategy.route(infoList, 3);
         System.out.println(route);

         System.out.println(lruStrategy.route(infoList, 2));
         System.out.println(lruStrategy.route(infoList, 4));
         System.out.println(lruStrategy.route(infoList, 1));

     }
}
