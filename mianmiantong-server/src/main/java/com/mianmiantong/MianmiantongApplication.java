package com.mianmiantong;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@MapperScan("com.mianmiantong.mapper")
public class MianmiantongApplication {

    private static final Logger log = LoggerFactory.getLogger(MianmiantongApplication.class);

    public static void main(String[] args) {
        // 1. 先直接设置系统属性（兜底方案）
        loadDotenv();

        // 2. 启动时验证关键属性
        log.info("=== 诊断: DEEPSEEK_API_KEY ===");
        log.info("System.getProperty: {}", System.getProperty("DEEPSEEK_API_KEY", "<NOT SET>"));
        log.info("System.getenv:        {}", System.getenv().getOrDefault("DEEPSEEK_API_KEY", "<NOT SET>"));

        SpringApplication.run(MianmiantongApplication.class, args);
    }

    private static void loadDotenv() {
        Path current = Paths.get(".").toAbsolutePath().normalize();
        log.info("Working directory: {}", current);
        for (int i = 0; i < 3; i++) {
            Path candidate = current.resolve(".env");
            log.info("Checking: {}", candidate);
            if (Files.exists(candidate)) {
                log.info("Found .env at: {}", candidate);
                Dotenv dotenv = Dotenv.configure().directory(current.toString()).load();
                dotenv.entries().forEach(e -> {
                    System.setProperty(e.getKey(), e.getValue());
                    log.info("  Set {} = {}...", e.getKey(),
                            e.getValue().isEmpty() ? "<EMPTY>" :
                            e.getValue().substring(0, Math.min(8, e.getValue().length())));
                });
                return;
            }
            current = current.getParent();
            if (current == null) break;
        }
        log.error(".env file NOT FOUND");
    }
}
