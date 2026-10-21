package com.sovereign.sentinel;

import java.time.Clock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SentinelApplication {
    public static void main(String[] args) throws IOException {
        // Ensure the parent folder exists before H2 creates the database file.
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(SentinelApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
