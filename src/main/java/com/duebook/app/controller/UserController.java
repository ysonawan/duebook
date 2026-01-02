package com.duebook.app.controller;

import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.dto.RequestOtpForEmailChangeRequest;
import com.duebook.app.dto.UpdateBasicInfoRequest;
import com.duebook.app.dto.UpdateBasicInfoWithOtpRequest;
import com.duebook.app.dto.UpdateSecondaryEmailsRequest;
import com.duebook.app.dto.UpdateSecondaryEmailsWithOtpRequest;
import com.duebook.app.dto.UserProfileDTO;
import com.duebook.app.service.UserService;
import com.duebook.app.exception.ApplicationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        UserProfileDTO profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile/secondary-emails")
    public ResponseEntity<UserProfileDTO> updateSecondaryEmails(
            @Valid @RequestBody UpdateSecondaryEmailsRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        UserProfileDTO updatedProfile = userService.updateSecondaryEmails(user.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/profile/basic")
    public ResponseEntity<UserProfileDTO> updateBasicInfo(
            @RequestBody UpdateBasicInfoRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));
        UserProfileDTO updatedProfile = userService.updateBasicInfo(user.getId(), request);
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

        userService.requestOtpForEmailChange(user.getId(), request);
        return ResponseEntity.ok("OTP has been sent to your new email address. Please verify it to update your primary email.");
    }

    @PostMapping("/profile/request-otp-for-secondary-emails")
    public ResponseEntity<String> requestOtpForSecondaryEmailChange(
            @RequestBody Map<String, List<String>> request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        List<String> secondaryEmails = request.getOrDefault("secondaryEmails", new ArrayList<>());
        userService.requestOtpForSecondaryEmailChange(user.getId(), secondaryEmails);
        return ResponseEntity.ok("OTP has been sent to your newly added email address for verification.");
    }

    @PutMapping("/profile/basic-with-otp")
    public ResponseEntity<UserProfileDTO> updateBasicInfoWithOtp(
            @Valid @RequestBody UpdateBasicInfoWithOtpRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));
        UserProfileDTO updatedProfile = userService.updateBasicInfoWithOtp(user.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/profile/secondary-emails-with-otp")
    public ResponseEntity<UserProfileDTO> updateSecondaryEmailsWithOtp(
            @Valid @RequestBody UpdateSecondaryEmailsWithOtpRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));

        UserProfileDTO updatedProfile = userService.updateSecondaryEmailsWithOtp(user.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }
}
