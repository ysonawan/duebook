package com.duebook.app.controller;

import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.dto.RequestOtpForEmailChangeRequest;
import com.duebook.app.dto.UpdateBasicInfoRequest;
import com.duebook.app.dto.UpdateBasicInfoWithOtpRequest;
import com.duebook.app.dto.UserProfileDTO;
import com.duebook.app.service.UserService;
import com.duebook.app.exception.ApplicationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        log.debug("Fetching profile for user ID: {}", user.getId());
        UserProfileDTO profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile/basic")
    public ResponseEntity<UserProfileDTO> updateBasicInfo(
            @RequestBody UpdateBasicInfoRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));
        log.info("Updating basic info for user ID: {}", user.getId());
        UserProfileDTO updatedProfile = userService.updateBasicInfo(user.getId(), request);
        log.info("Basic info updated successfully for user ID: {}", user.getId());
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/profile/request-otp-for-primary-email")
    public ResponseEntity<String> requestOtpForPrimaryEmailChange(
            @Valid @RequestBody RequestOtpForEmailChangeRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        log.info("OTP requested for email change for user ID: {}", user.getId());
        userService.requestOtpForEmailChange(user.getId(), request);
        return ResponseEntity.ok("OTP has been sent to your new email address. Please verify it to update your primary email.");
    }

    @PutMapping("/profile/basic-with-otp")
    public ResponseEntity<UserProfileDTO> updateBasicInfoWithOtp(
            @Valid @RequestBody UpdateBasicInfoWithOtpRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));
        log.info("Updating basic info with OTP for user ID: {}", user.getId());
        UserProfileDTO updatedProfile = userService.updateBasicInfoWithOtp(user.getId(), request);
        log.info("Basic info with OTP updated successfully for user ID: {}", user.getId());
        return ResponseEntity.ok(updatedProfile);
    }
}
