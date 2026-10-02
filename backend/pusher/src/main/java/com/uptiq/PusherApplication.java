package com.uptiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication()
@EnableScheduling
@EntityScan("com.uptiq.entity")
@EnableJpaRepositories("com.uptiq.repository")
public class PusherApplication {
    public static void main(String[] args) {
        SpringApplication.run(PusherApplication.class, args);
    }
}