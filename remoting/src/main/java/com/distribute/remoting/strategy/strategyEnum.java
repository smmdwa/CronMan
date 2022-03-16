package com.distribute.remoting.strategy;


public enum strategyEnum {

    shardStrategy("shard",new ShardStrategy()),
    randomStrategy("random",new RandomStrategy()),
    lruStrategy("lru",new lruStrategy());

    private String name;

    public String getName(){return this.name;}
    private Strategy strategy;

    public Strategy getStrategy(){return this.strategy;}

    strategyEnum(String name, Strategy strategy){
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