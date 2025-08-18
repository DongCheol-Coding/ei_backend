package com.example.ei_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class EiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EiBackendApplication.class, args);
    }

}
