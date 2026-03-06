package com.arthasmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class ArthasManagerApplication {

    public static void main(String[] args) throws Exception {
        // Ensure the SQLite DB directory exists before Spring initializes the DataSource
        Path dbDir = Path.of(System.getProperty("user.home"), ".arthas-manager");
        Files.createDirectories(dbDir);
        SpringApplication.run(ArthasManagerApplication.class, args);
    }
}
