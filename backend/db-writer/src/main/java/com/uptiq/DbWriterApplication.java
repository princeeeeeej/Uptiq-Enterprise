package com.uptiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        excludeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
        }
)
public class DbWriterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbWriterApplication.class, args);
    }
}