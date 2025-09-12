package com.example.ei_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.example.ei_backend")
public class EiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EiBackendApplication.class, args);
    }

    @PostConstruct
    public void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
