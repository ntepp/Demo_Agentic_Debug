package com.demo.financial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialDemoApplication.class, args);
    }
}
