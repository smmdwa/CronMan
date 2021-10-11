package com.distribute.executor.annotation;


import com.distribute.remoting.bean.job;
import com.distribute.remoting.utils.idUtil;
import org.springframework.stereotype.Component;


@Component
public class test implements job {
    @Override
    @scheduleJob(name = "execute")
    public void execute() {
        System.out.println(name+"执行=========");
    }

    String name="111";

    public static void main(String[] args) {
        idUtil.id.nextId();
        System.out.println(new idUtil().nextId());
        System.out.println(new idUtil().nextId());
    }

    public void tryit(){
        System.out.println("我只是试一试==========="+name);
    }

    public void doit(){
        System.out.println("我只是做一做==========="+name);
    }

}
