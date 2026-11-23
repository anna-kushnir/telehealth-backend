package com.telehealth.auth.service;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.AuthResult;
import com.telehealth.auth.dto.response.JwtResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuthService {

    void registerUser(RegisterRequest registerRequest);
    UUID registerDoctorByAdmin(String email, String tempPassword);
    AuthResult loginUser(LoginRequest loginRequest, String dpopHeader);
    JwtResponse verify2faCode(Verify2faRequest verify2faRequest, String dpopHeader);
    boolean isDoctorExists(UUID doctorId);
    List<Map<String, Object>> getAllPatients();
    void sendActionConfirmationOtp();
    void verifyActionOtpForCurrentUser(String otpCode);
}
