package com.telehealth.anonymization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnonymizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnonymizationServiceApplication.class, args);
    }

}
