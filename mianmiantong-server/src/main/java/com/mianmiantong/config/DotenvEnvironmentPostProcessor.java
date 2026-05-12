package com.mianmiantong.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path current = Paths.get(".").toAbsolutePath().normalize();
        for (int i = 0; i < 3; i++) {
            Path candidate = current.resolve(".env");
            if (Files.exists(candidate)) {
                log.info("Loading .env from: {}", candidate);
                Dotenv dotenv = Dotenv.configure().directory(current.toString()).load();

                Map<String, Object> props = new HashMap<>();
                dotenv.entries().forEach(e -> props.put(e.getKey(), e.getValue()));
                environment.getPropertySources()
                        .addFirst(new MapPropertySource("dotenv", props));
                return;
            }
            current = current.getParent();
            if (current == null) break;
        }
        log.warn(".env file not found in current directory or up to 2 parent levels");
    }
}
