package com.distribute.remoting;

import com.distribute.remoting.mapper.JobMapper;
import com.distribute.remoting.netty_server.NettyServer;
import com.distribute.remoting.netty_server.nameServerController;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

//@MapperScan("com.distribute.remoting.mapper")
@SpringBootApplication
public class RemotingApplication {

    public static void main(String[] args) throws InterruptedException {

        ConfigurableApplicationContext context = SpringApplication.run(RemotingApplication.class, args);

//        Arrays.stream(context.getBeanDefinitionNames()).forEach(System.out::println);
    }

}
