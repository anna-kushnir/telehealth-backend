package com.auth.mfa.totp.controller;

import com.auth.mfa.totp.dto.request.EnrollRequest;
import com.auth.mfa.totp.dto.request.VerifyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/factors/totp")
public class TotpMfaController {

    @Value("${mfa.issuer}")
    private String issuer;

    @Value("${mfa.internal-api-key}")
    private String internalApiKey;

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final Map<String, String> secretStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostMapping("/enroll")
    public ResponseEntity<?> enrollTotp(
            @RequestHeader(value = "X-MFA-Api-Key", required = false) String apiKey,
            @RequestBody EnrollRequest request) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA API key"));
        }

        String secret = generateBase32Secret(20);
        secretStore.put(request.getSubject(), secret);
        String otpAuthUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, request.getAccountName(), secret, issuer);

        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "otpAuthUri", otpAuthUri,
                "amr", "otp"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyTotp(
            @RequestHeader(value = "X-MFA-Api-Key", required = false) String apiKey,
            @RequestBody VerifyRequest request) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA API key"));
        }

        String secret = secretStore.get(request.getSubject());
        if (secret == null || request.getCode() == null || !validateTotpCode(secret, request.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Invalid TOTP authenticator code"));
        }

        return ResponseEntity.ok(Map.of("verified", true, "amr", "otp"));
    }

    private String generateBase32Secret(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE32_CHARS.charAt(random.nextInt(BASE32_CHARS.length())));
        }
        return sb.toString();
    }

    private boolean validateTotpCode(String base32Secret, String code) {
        long currentBucket = Instant.now().getEpochSecond() / 30L;
        for (long i = -1; i <= 1; i++) {
            if (generateCode(base32Secret, currentBucket + i).equals(code.trim())) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String base32Secret, long timeBucket) {
        try {
            byte[] key = decodeBase32(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeBucket).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP calculation failed", e);
        }
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, count = 0;
        for (char c : clean.toCharArray()) {
            buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
