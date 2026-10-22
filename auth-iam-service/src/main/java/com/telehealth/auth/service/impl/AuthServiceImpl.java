package com.telehealth.auth.service.impl;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.AuthResult;
import com.telehealth.auth.dto.response.JwtResponse;
import com.telehealth.auth.model.Role;
import com.telehealth.auth.model.User;
import com.telehealth.auth.repository.RoleRepository;
import com.telehealth.auth.repository.UserRepository;
import com.telehealth.auth.security.JwtUtils;
import com.telehealth.auth.security.UserDetailsImpl;
import com.telehealth.auth.service.AuthService;
import com.telehealth.auth.service.EmailService;
import kpi.kushnir.privacy.neo4j.UserIdentityNode;
import kpi.kushnir.privacy.neo4j.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

//    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserIdentityRepository userIdentityRepository;

    @Override
    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .isActive(true)
                .is2faEnabled(registerRequest.isEnable2fa())
                .roles(new HashSet<>())
                .build();

        Role userRole = roleRepository.findByName(registerRequest.getRole())
                .orElseThrow(() -> new RuntimeException("Role is not found."));

        user.getRoles().add(userRole);
        userRepository.save(user);
        user = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User was not created."));
        userIdentityRepository.save(new UserIdentityNode(user.getId().toString()));
    }

    @Override
    @Transactional
    public AuthResult loginUser(LoginRequest loginRequest, String dpopHeader) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByEmail(Objects.requireNonNull(userDetails).getUsername())
                .orElseThrow(() -> new RuntimeException("User is not found."));

        if (user.is2faEnabled()) {
            String code = String.format("%06d", new Random().nextInt(999999));
            user.setEmailAuthCode(code);
            user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            emailService.send2faCode(user.getEmail(), code);

            return AuthResult.builder()
                    .requires2fa(true)
                    .message("Verification code was sent on your email.")
                    .build();
        }

        return AuthResult.builder()
                .requires2fa(false)
                .jwtResponse(generateJwtResponse(authentication, userDetails, dpopHeader, List.of("pwd")))
                .build();
    }

    private JwtResponse generateJwtResponse(Authentication authentication,
                                            UserDetailsImpl userDetails,
                                            String dpopHeader,
                                            List<String> amrMethods) {
        String jwt = jwtUtils.generateDpopBoundToken(authentication, dpopHeader, amrMethods);

        List<String> roles = Objects.requireNonNull(userDetails).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .token(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .roles(roles)
                .type("DPoP")
                .build();
    }

    @Override
    @Transactional
    public JwtResponse verify2faCode(Verify2faRequest verify2faRequest, String dpopHeader) {
        User user = userRepository.findByEmail(verify2faRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        if (!user.is2faEnabled()) {
            throw new IllegalArgumentException("2FA is disabled for this user.");
        }

        if (user.getEmailAuthCode() == null || !user.getEmailAuthCode().equals(verify2faRequest.getCode())) {
            throw new IllegalStateException("Incorrect verification code.");
        }

        if (user.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification code expired.");
        }

        user.setEmailAuthCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return generateJwtResponse(authentication, userDetails, dpopHeader, List.of("pwd", "otp"));
    }
}
