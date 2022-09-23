package com.example.requestdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RequestDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestDemoApplication.class, args);
    }

}
