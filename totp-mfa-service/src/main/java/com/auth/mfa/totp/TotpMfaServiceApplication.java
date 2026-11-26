package com.auth.mfa.totp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TotpMfaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TotpMfaServiceApplication.class, args);
    }

}
