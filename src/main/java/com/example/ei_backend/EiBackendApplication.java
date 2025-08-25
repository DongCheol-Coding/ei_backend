package com.example.ei_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.example.ei_backend")
public class EiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EiBackendApplication.class, args);
    }

}
