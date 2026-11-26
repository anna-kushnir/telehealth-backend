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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

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
                                              @RequestHeader(value = "DPoP") String dpopHeader) {
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
                                           @RequestHeader(value = "DPoP") String dpopHeader) {
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
    public ResponseEntity<?> deleteMyAccount(@RequestParam String otp) {
        try {
            gdprErasureService.executeRightToErasure(otp);
            return ResponseEntity.ok(Map.of("message", "All personal data has been permanently erased from the system."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete account: " + e.getMessage()));
        }
    }

    @GetMapping("/doctors/{id}/exists")
    public ResponseEntity<Boolean> checkDoctorExists(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.isDoctorExists(id));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestActionOtp() {
        try {
            authService.sendActionConfirmationOtp();
            return ResponseEntity.ok(Map.of("message", "Verification code sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createDoctorByAdmin(@RequestBody Map<String, String> request) {
        try {
            UUID doctorId = authService.registerDoctorByAdmin(request.get("email"), request.get("password"));
            return ResponseEntity.ok(Map.of("doctorId", doctorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/patients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPatientsForAdmin() {
        return ResponseEntity.ok(authService.getAllPatients());
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserByAdmin(@PathVariable UUID id, @RequestParam String otp) {
        try {
            gdprErasureService.executeErasureByAdmin(id, otp);
            return ResponseEntity.ok(Map.of("message", "User deleted and cryptographic keys revoked."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<?> get2faStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", authService.isCurrentUser2faEnabled(),
                "factor", authService.getCurrentUserMfaFactor()));
    }

    @PatchMapping("/2fa")
    public ResponseEntity<?> toggle2fa(@RequestBody Map<String, Boolean> request) {
        try {
            boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
            authService.setCurrentUser2fa(enabled);
            return ResponseEntity.ok(Map.of("enabled", enabled,
                    "message", "2FA settings updated. Please re-login to upgrade your session token."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mfa/totp/enroll")
    public ResponseEntity<?> enrollTotp() {
        try {
            return ResponseEntity.ok(authService.enrollTotpForCurrentUser());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/mfa/factor")
    public ResponseEntity<?> switchMfaFactor(@RequestBody Map<String, String> request) {
        try {
            authService.switchMfaFactor(request.get("factor"));
            return ResponseEntity.ok(Map.of("message", "Preferred MFA factor updated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
