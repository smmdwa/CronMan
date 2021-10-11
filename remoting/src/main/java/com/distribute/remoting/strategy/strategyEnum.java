package com.distribute.remoting.strategy;

import lombok.Data;


public enum strategyEnum {

    shardStrategy("shard",new shardStrategy()),
    randomStrategy("random",new randomStrategy()),
    lruStrategy("lru",new lruStrategy());

    private String name;

    public String getName(){return this.name;}
    private strategy strategy;

    public strategy getStrategy(){return this.strategy;}

    strategyEnum(String name,strategy strategy){
        this.name=name;
        this.strategy=strategy;
    }
    public static strategyEnum match(String name){
        for (strategyEnum item: strategyEnum.values()) {
            if(item.name.equals(name)){
                return item;
            }
        }
        //默认用random
        return randomStrategy;
    }
}