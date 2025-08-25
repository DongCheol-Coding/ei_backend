package com.example.ei_backend.config;

import com.example.ei_backend.util.AppFrontProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(AppFrontProperties.class)
@Configuration
public class AppConfig {
}
