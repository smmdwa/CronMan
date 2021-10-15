package com.distribute.executor.bean;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class methodWorker extends worker {

    private final Object target;
    private final Method method;
    private Method initMethod;
    private Method destroyMethod;
    private Object[] args;

    public void setArgs(List<Object> args){
        this.args= args.toArray();
    }

    public methodWorker(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;

        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public int execute() throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0 && args!=null) {
//            method.invoke(target, new Object[paramTypes.length]);       // method-param can not be primitive-types
            method.invoke(target, this.args);       // method-param can not be primitive-types

        } else {
            method.invoke(target);
        }
        log.info("execute!");
        return 1;
    }

    @Override
    public void init() throws Exception {
        if(initMethod != null) {
            initMethod.invoke(target);
        }
    }

    @Override
    public void destroy() throws Exception {
        if(destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString()+"["+ target.getClass() + "#" + method.getName() +"]";
    }
}
