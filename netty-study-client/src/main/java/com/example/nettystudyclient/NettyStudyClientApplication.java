package com.example.nettystudyclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NettyStudyClientApplication {

    public static void main(String[] args) {
        final ConfigurableApplicationContext applicationContext = SpringApplication.run(NettyStudyClientApplication.class, args);
        final NettyClient nettyClient = applicationContext.getBean(NettyClient.class);
        nettyClient.run();
    }

}
