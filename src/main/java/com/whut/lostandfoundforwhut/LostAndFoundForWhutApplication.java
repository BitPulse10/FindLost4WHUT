package com.whut.lostandfoundforwhut;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 应用启动类，负责启动 Spring Boot 应用
 */
@MapperScan("com.whut.lostandfoundforwhut.mapper")
@SpringBootApplication
@EnableScheduling
public class LostAndFoundForWhutApplication {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 应用入口，启动 Spring Boot
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LostAndFoundForWhutApplication.class, args);
    }

}
