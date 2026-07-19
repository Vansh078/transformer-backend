package com.smart.transformer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TransformerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransformerApplication.class, args);
    }
}
