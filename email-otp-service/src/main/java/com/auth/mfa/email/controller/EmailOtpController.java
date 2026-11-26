package com.auth.mfa.email.controller;

import com.auth.mfa.email.dto.request.ChallengeRequest;
import com.auth.mfa.email.dto.request.VerifyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/factors/email-otp")
@RequiredArgsConstructor
public class EmailOtpController {

    private final JavaMailSender mailSender;

    @Value("${mfa.internal-api-key}")
    private String internalApiKey;

    private record OtpEntry(String code, LocalDateTime expiresAt) {}

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostMapping("/challenge")
    public ResponseEntity<?> sendChallenge(
            @RequestHeader(value = "X-MFA-Api-Key", required = false) String apiKey,
            @RequestBody ChallengeRequest request) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA API key"));
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(request.getSubject(), new OtpEntry(code, LocalDateTime.now().plusMinutes(5)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getDestination());
        message.setSubject("Verification Code");
        message.setText("Your one-time verification code is: " + code + "\nDo not share this code with anyone. It expires in 5 minutes.");
        mailSender.send(message);

        return ResponseEntity.ok(Map.of("status", "CHALLENGE_SENT", "amr", "email_otp"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyChallenge(
            @RequestHeader(value = "X-MFA-Api-Key", required = false) String apiKey,
            @RequestBody VerifyRequest request) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA API key"));
        }

        OtpEntry entry = otpStore.get(request.getSubject());
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now()) || !entry.code().equals(request.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Invalid or expired OTP code"));
        }

        otpStore.remove(request.getSubject());
        return ResponseEntity.ok(Map.of("verified", true, "amr", "email_otp"));
    }
}
