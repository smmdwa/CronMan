package com.distribute.remoting.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class helloService {

    public void hello(){
        log.info("hello!");
    }
}
