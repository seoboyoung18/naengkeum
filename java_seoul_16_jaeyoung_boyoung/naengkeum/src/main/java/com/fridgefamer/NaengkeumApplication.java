package com.fridgefamer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fridgefamer.mapper")
public class NaengkeumApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaengkeumApplication.class, args);
    }
}