package com.distribute.executor.annotation;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Aspect
public class scheduleJobAspect {

    @Pointcut("@annotation(com.distribute.executor.annotation.scheduleJob)")
    public void pointcut() {
    }

//    @Before("pointcut()")
//    public void doBefore(JoinPoint joinPoint) {
//        try {
//            //*========控制台输出=========*//
//            System.out.println("=====前置通知开始=====");
//            System.out.println("###请求方法:" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()"));
////            System.out.println("== 请求参数： "+job.name());
//
//            //1.获取到所有的参数值的数组
//            Object[] args = joinPoint.getArgs();
//            Signature signature = joinPoint.getSignature();
//            MethodSignature methodSignature = (MethodSignature) signature;
//            //2.获取到方法的所有参数名称的字符串数组
//            String[] parameterNames = methodSignature.getParameterNames();
//            Method method = methodSignature.getMethod();
//            System.out.println("---------------参数列表开始-------------------------");
//            for (int i =0 ,len=parameterNames.length;i < len ;i++){
//                System.out.println("参数名："+ parameterNames[i] + " = " +args[i]);
//            }
//            System.out.println("---------------参数列表结束-------------------------");
//
//            scheduleJob myJob= (scheduleJob) method.getAnnotation(scheduleJob.class);
//
//            System.out.println("自定义注解 key:" + myJob.name());
//
//
//
//            System.out.println("=====前置通知结束=====");
//        }  catch (Exception e) {
//            //记录本地异常日志
//            e.printStackTrace();
//        }
//    }
//    @After("pointcut()")
//    public void doAfterTask(JoinPoint joinPoint){
//        System.out.println("=====后置通知=====");
//        System.out.println("###请求方法:" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()"));
//        System.out.println("=====后置通知end=====");
//    }


}
