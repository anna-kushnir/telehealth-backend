package com.telehealth.auth.service;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.AuthResult;
import com.telehealth.auth.dto.response.JwtResponse;

public interface AuthService {

    void registerUser(RegisterRequest registerRequest);
    AuthResult loginUser(LoginRequest loginRequest);
    JwtResponse verify2faCode(Verify2faRequest verify2faRequest);
}
