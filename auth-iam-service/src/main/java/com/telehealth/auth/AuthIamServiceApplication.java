package com.telehealth.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.telehealth.auth.model")
public class AuthIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthIamServiceApplication.class, args);
    }

}
