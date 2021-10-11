package com.distribute.remoting.bean;


import com.distribute.remoting.utils.Context;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

//@AllArgsConstructor
//@Data
//public class Invocation implements Serializable {
//
//    private String className;
//
//    private String methodName;
//
//    private List<String> parameterTypes;
//
//    private List<String> args;
//
////    private static final long serialVersionUID = -7005478531563162356L;
////    private Class targetClass;
////
////    private String methodName;
////
////    private Class[] parameterTypes;
////
////    private Object[] args;
////
////    public Invocation() {
////
////    }
////
////    public Invocation(Class targetClass, String methodName, Class[] parameterTypes, Object... args) {
////        this.methodName = methodName;
////        this.parameterTypes = parameterTypes;
////        this.targetClass = targetClass;
////        this.args = args;
////    }
////
////
////    public Object[] getArgs() {
////        return args;
////    }
////
////    public Class getTargetClass() {
////        return targetClass;
////    }
////
////    public String getMethodName() {
////        return methodName;
////    }
////
////    public Class[] getParameterTypes() {
////        return parameterTypes;
////    }
////
////    public Object invoke() throws Exception {
////        Object target = null;
////        try {
////            target = Context.getBean(targetClass);
////        } catch(NoSuchBeanDefinitionException e) {
////            target = Class.forName(targetClass.getName());
////        }
////        Method method = target.getClass().getMethod(methodName,parameterTypes);
////        // 调用服务方法
////        return method.invoke(targetClass.newInstance(), args);
////    }
//
//
//}