package com.telehealth.auth.controller;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.AuthResult;
import com.telehealth.auth.dto.response.JwtResponse;
import com.telehealth.auth.service.AuthService;
import com.telehealth.auth.service.GdprErasureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GdprErasureService gdprErasureService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerUser(registerRequest);
            return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              @RequestHeader(value = "DPoP", required = true) String dpopHeader) {
        try {
            AuthResult result = authService.loginUser(loginRequest, dpopHeader);
            if (result.isRequires2fa()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", result.getMessage()));
            }
            return ResponseEntity.ok(result.getJwtResponse());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2faCode(@Valid @RequestBody Verify2faRequest verify2faRequest,
                                           @RequestHeader(value = "DPoP", required = true) String dpopHeader) {
        try {
            JwtResponse jwtResponse = authService.verify2faCode(verify2faRequest, dpopHeader);
            return ResponseEntity.ok(jwtResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteMyAccount() {
        try {
            gdprErasureService.executeRightToErasure();
            return ResponseEntity.ok(Map.of("message", "All personal data has been permanently erased from the system."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete account: " + e.getMessage()));
        }
    }
}
