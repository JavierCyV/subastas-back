package com.subastas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubastasApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubastasApplication.class, args);
    }
}
