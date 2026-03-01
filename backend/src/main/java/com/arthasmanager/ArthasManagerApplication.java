package com.arthasmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArthasManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthasManagerApplication.class, args);
    }
}
