package com.telehealth.auth.service;

public interface EmailService {

    void send2faCode(String email, String code);
}
