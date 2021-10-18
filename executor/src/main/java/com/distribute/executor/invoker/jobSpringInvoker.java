package com.distribute.executor.invoker;

import com.distribute.executor.annotation.scheduleJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Component
@Data
public class jobSpringInvoker extends jobInvoker implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {


    @PostConstruct
    public void initialize(){
    }
    // start
    @Override
    public void afterSingletonsInstantiated() {
        log.info("afterSingletonsInstantiated ");

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(applicationContext);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // destroy
    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        // init job handler from method
        //getBeanNamesForType 根据class类型筛选出容器中所有子类的名字(剔除掉没有其他别名的类),
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);

            Map<Method, scheduleJob> annotatedMethods = null;   // referred to ：org.springframework.context.event.EventListenerMethodProcessor.processBean
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<scheduleJob>() {
                            @Override
                            public scheduleJob inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, scheduleJob.class);
                            }
                        });
            } catch (Throwable ex) {
                log.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", ex);
            }
            if (annotatedMethods==null || annotatedMethods.isEmpty()) {
                continue;
            }

            for (Map.Entry<Method, scheduleJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                scheduleJob scheduleJob = methodXxlJobEntry.getValue();
                // regist
                registAnnotationWorker(scheduleJob, bean, executeMethod);
            }
        }
        log.info("initJobHandlerMethodRepository");
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
