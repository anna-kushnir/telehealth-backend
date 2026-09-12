package com.telehealth.auth.controller;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.JwtResponse;
import com.telehealth.auth.model.Role;
import com.telehealth.auth.model.User;
import com.telehealth.auth.repository.RoleRepository;
import com.telehealth.auth.repository.UserRepository;
import com.telehealth.auth.security.JwtUtils;
import com.telehealth.auth.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already taken!");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .isActive(true)
                .is2faEnabled(registerRequest.isEnable2fa())
                .roles(new HashSet<>())
                .build();

        Role userRole = roleRepository.findByName(registerRequest.getRole())
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().add(userRole);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByEmail(Objects.requireNonNull(userDetails).getUsername())
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        if (user.is2faEnabled()) {
            String code = String.format("%06d", new Random().nextInt(999999));
            user.setEmailAuthCode(code);
            user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // TODO: додати відправку коду на email
            System.out.println("MOCK EMAIL: Your verification code is - " + code);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Verification code was sent on your email.");
        }

        return generateJwtResponce(authentication, userDetails);
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2faCode(@Valid @RequestBody Verify2faRequest verify2faRequest) {
        User user = userRepository.findByEmail(verify2faRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        if (!user.is2faEnabled()) {
            return ResponseEntity.badRequest().body("2FA is disabled for this user.");
        }

        if (user.getEmailAuthCode() == null || !user.getEmailAuthCode().equals(verify2faRequest.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect verification code.");
        }

        if (user.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Verification code expired.");
        }

        user.setEmailAuthCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return generateJwtResponce(authentication, userDetails);
    }

    private ResponseEntity<?> generateJwtResponce(Authentication authentication, UserDetailsImpl userDetails) {
        String jwt = jwtUtils.generateJwtToken(authentication);

        List<String> roles = Objects.requireNonNull(userDetails).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .roles(roles)
                .type("Bearer")
                .build());
    }
}
