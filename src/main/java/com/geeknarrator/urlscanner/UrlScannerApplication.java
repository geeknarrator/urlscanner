package com.geeknarrator.urlscanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrlScannerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(UrlScannerApplication.class)
                .web(org.springframework.boot.WebApplicationType.SERVLET) // Explicitly set the web application type
                .run(args);
    }
}
