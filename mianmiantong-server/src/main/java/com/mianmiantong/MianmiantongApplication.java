package com.mianmiantong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mianmiantong.mapper")
public class MianmiantongApplication {
    public static void main(String[] args) {
        SpringApplication.run(MianmiantongApplication.class, args);
    }
}
