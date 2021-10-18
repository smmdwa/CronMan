package com.distribute.remoting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

//@MapperScan("com.distribute.remoting.mapper")
@SpringBootApplication
public class RemotingApplication {

    public static void main(String[] args) throws InterruptedException {

        ConfigurableApplicationContext context = SpringApplication.run(RemotingApplication.class, args);

//        Arrays.stream(context.getBeanDefinitionNames()).forEach(System.out::println);
    }

}
