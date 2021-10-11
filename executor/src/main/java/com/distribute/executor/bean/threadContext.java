package com.distribute.executor.bean;


import lombok.Data;

//归线程所有，每个线程拥有的context不一样，因为是放在InheritableThreadLocal里面的
//所以不需要单独放进jobThread里面
@Data
public class threadContext {

    private final long jobId;
    /**
     * shard index
     */
    private final int shardIndex;

    /**
     * shard total
     */
    private final int shardTotal;

    private static InheritableThreadLocal<threadContext> contextHolder = new InheritableThreadLocal<threadContext>(); // support for child thread of job handler)

    public threadContext(long jobId, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
    }


    public static void setXxlJobContext(threadContext threadContext){
        contextHolder.set(threadContext);
    }

    public static threadContext getExecutorContext(){
        return contextHolder.get();
    }
}
