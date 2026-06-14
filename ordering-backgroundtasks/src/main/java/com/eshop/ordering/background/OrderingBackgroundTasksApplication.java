package com.eshop.ordering.background;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OrderingBackgroundTasksApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderingBackgroundTasksApplication.class, args);
    }
}
