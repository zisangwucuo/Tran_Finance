package com.transfinance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业转型金融数据平台后端启动入口。
 */
@MapperScan("com.transfinance.mapper")
@SpringBootApplication
public class TransFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransFinanceApplication.class, args);
    }
}
