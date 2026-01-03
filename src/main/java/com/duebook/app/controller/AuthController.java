package com.duebook.app.controller;

import com.duebook.app.dto.AuthResponse;
import com.duebook.app.dto.LoginRequest;
import com.duebook.app.dto.OtpRequest;
import com.duebook.app.dto.OtpVerifyRequest;
import com.duebook.app.dto.SignupRequest;
import com.duebook.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup attempt for phone: {}", request.getPhone());
        AuthResponse response = authService.signup(request);
        log.info("User signed up successfully with ID: {} and phone: {}", response.getUserId(), request.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhone());
        AuthResponse response = authService.login(request);
        log.info("User logged in successfully with ID: {} and phone: {}", response.getUserId(), request.getPhone());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody OtpRequest request) {
        log.info("OTP requested for email: {}", request.getEmail());
        authService.requestOtp(request.getEmail());
        log.info("OTP sent successfully to email: {}", request.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent to your email");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("OTP verification attempted for email: {}", request.getEmail());
        AuthResponse response = authService.loginWithOtp(request.getEmail(), request.getOtp());
        log.info("OTP verified successfully for user ID: {} with email: {}", response.getUserId(), request.getEmail());
        return ResponseEntity.ok(response);
    }
}
