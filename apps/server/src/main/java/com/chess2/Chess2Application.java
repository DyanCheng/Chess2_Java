package com.chess2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Chess2Application {
    public static void main(String[] args) {
        SpringApplication.run(Chess2Application.class, args);
    }
}
