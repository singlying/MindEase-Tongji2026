package com.mindease.playground;

import com.mindease.playground.config.PlaygroundProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PlaygroundProperties.class)
public class DashscopeVoicePlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashscopeVoicePlaygroundApplication.class, args);
    }
}
