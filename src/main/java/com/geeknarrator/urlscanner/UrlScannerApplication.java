package com.geeknarrator.urlscanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrlScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlScannerApplication.class, args);
    }
}