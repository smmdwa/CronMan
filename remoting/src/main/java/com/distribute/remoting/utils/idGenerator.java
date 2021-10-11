package com.distribute.remoting.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class idGenerator {
    private static final AtomicInteger id = new AtomicInteger();

    private static final AtomicLong idLong = new AtomicLong();


    public static int nextId() {
        return id.incrementAndGet();
    }


    public static long nextLongId(){ return idLong.incrementAndGet();}


}
