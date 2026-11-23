package com.telehealth.auth.service.impl;

import com.telehealth.auth.dto.request.LoginRequest;
import com.telehealth.auth.dto.request.RegisterRequest;
import com.telehealth.auth.dto.request.Verify2faRequest;
import com.telehealth.auth.dto.response.AuthResult;
import com.telehealth.auth.dto.response.JwtResponse;
import com.telehealth.auth.model.Role;
import com.telehealth.auth.model.RoleName;
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
import java.util.*;
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

        if (registerRequest.getRole() == RoleName.ROLE_ADMIN ||
                registerRequest.getRole() == RoleName.ROLE_DOCTOR) {
            throw new SecurityException("Self-registration for DOCTOR or ADMIN roles is forbidden.");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .isActive(true)
                .is2faEnabled(registerRequest.isEnable2fa())
                .roles(new HashSet<>())
                .build();

        Role userRole = roleRepository.findByName(RoleName.ROLE_PATIENT)
                .orElseThrow(() -> new RuntimeException("Role is not found."));

        user.getRoles().add(userRole);
        user = userRepository.save(user);
        userIdentityRepository.save(new UserIdentityNode(user.getId().toString()));
    }

    @Override
    @Transactional
    public UUID registerDoctorByAdmin(String email, String tempPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already taken.");
        }

        Role doctorRole = roleRepository.findByName(RoleName.ROLE_DOCTOR)
                .orElseThrow(() -> new RuntimeException("Doctor role not found"));

        User doctor = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .isActive(true)
                .is2faEnabled(true)
                .roles(Set.of(doctorRole))
                .build();

        doctor = userRepository.save(doctor);
        userIdentityRepository.save(new UserIdentityNode(doctor.getId().toString()));
        return doctor.getId();
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
            String code = String.format("%06d", new Random().nextInt(1_000_000));
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

    @Override
    public boolean isDoctorExists(UUID doctorId) {
        return userRepository.findById(doctorId)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_DOCTOR))
                .orElse(false);
    }

    @Override
    public List<Map<String, Object>> getAllPatients() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_PATIENT))
                .map(u -> Map.<String, Object>of("id", u.getId(), "email", u.getEmail()))
                .toList();
    }

    @Override
    @Transactional
    public void sendActionConfirmationOtp() {
        User currentUser = getCurrentAuthenticatedUser();
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        currentUser.setEmailAuthCode(code);
        currentUser.setCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(currentUser);

        emailService.send2faCode(currentUser.getEmail(), code);
    }

    @Override
    @Transactional
    public void verifyActionOtpForCurrentUser(String otpCode) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getEmailAuthCode() == null
                || !currentUser.getEmailAuthCode().equals(otpCode)
                || currentUser.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired verification code.");
        }
        currentUser.setEmailAuthCode(null);
        currentUser.setCodeExpiresAt(null);
        userRepository.save(currentUser);
    }

    private User getCurrentAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        String realUserId = jwtUtils.getUserNameFromJwtToken(token);
        return userRepository.findById(UUID.fromString(realUserId))
                .orElseThrow(() -> new RuntimeException("Current user not found."));
    }
}
