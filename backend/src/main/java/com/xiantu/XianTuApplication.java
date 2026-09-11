package com.xiantu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.xiantu.mapper")
public class XianTuApplication {

    public static void main(String[] args) {
        SpringApplication.run(XianTuApplication.class, args);
    }

}
