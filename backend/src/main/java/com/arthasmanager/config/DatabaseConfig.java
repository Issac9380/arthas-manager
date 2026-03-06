package com.arthasmanager.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Ensures the SQLite database file's parent directory exists before the DataSource connects.
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void ensureDbDirectory() {
        // jdbc:sqlite:/home/user/.arthas-manager/arthas-manager.db
        String path = datasourceUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(path);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("Created database directory: {}", parentDir.getAbsolutePath());
            }
        }
    }
}
