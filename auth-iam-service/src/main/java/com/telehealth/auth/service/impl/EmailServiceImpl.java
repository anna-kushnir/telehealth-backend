package com.telehealth.auth.service.impl;

import com.telehealth.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void send2faCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@telehealth.com");
        message.setTo(email);
        message.setSubject("Verification Code");
        message.setText("Hello!\n\nYour verification code is: " + code +
                "\n\nDo not share this code with anyone. The code will expire in 5 minutes. ");

        mailSender.send(message);
    }
}
