package com.distribute.executor.utils;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
// todo 作用就是从spring中获得spring的bean 比如@service 的 xximpl  调用它的某个方法，如 sendYanZhengMa()
//  触发一次
public class Context implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext contex)
            throws BeansException {
        System.out.println("Context set");
        context = contex;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static Object getBean(Class clazz) {
        return context.getBean(clazz);
    }

    public static <T> T getByTypeAndName(Class<T> clazz,String name) {
        Map<String,T> clazzMap = context.getBeansOfType(clazz);
        return clazzMap.get(name);
    }
}