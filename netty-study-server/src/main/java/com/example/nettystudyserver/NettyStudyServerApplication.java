package com.example.nettystudyserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NettyStudyServerApplication {

    public static void main(String[] args) {
        final ConfigurableApplicationContext applicationContext = SpringApplication.run(NettyStudyServerApplication.class, args);
        final NettyService nettyService = applicationContext.getBean(NettyService.class);
        nettyService.run();
    }

}
