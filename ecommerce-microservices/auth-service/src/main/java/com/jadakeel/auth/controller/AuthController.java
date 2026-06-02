package com.jadakeel.auth.controller;

import com.jadakeel.auth.dto.*;
import com.jadakeel.auth.model.UserAccount;
import com.jadakeel.auth.repository.UserAccountRepository;
import com.jadakeel.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserAccountRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        UserAccount user = UserAccount.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole() == null ? UserAccount.Role.CUSTOMER : request.getRole())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        UserAccount saved = userRepository.save(user);
        return toResponse(saved, jwtService.generateToken(saved));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return toResponse(user, jwtService.generateToken(user));
    }

    @PostMapping("/validate-token")
    public Map<String, Boolean> validateToken(@Valid @RequestBody TokenRequest request) {
        return Map.of("valid", jwtService.isValid(request.getToken()));
    }

    private AuthResponse toResponse(UserAccount user, String token) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
