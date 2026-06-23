package com.shima.diary;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 拾码日记 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.shima.diary.mapper")
public class ShimaDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShimaDiaryApplication.class, args);
    }
}