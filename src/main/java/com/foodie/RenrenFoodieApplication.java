package com.foodie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.foodie.mapper")
public class RenrenFoodieApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenrenFoodieApplication.class, args);
    }

}
