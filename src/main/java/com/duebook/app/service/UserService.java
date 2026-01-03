package com.duebook.app.service;

import com.duebook.app.dto.RequestOtpForEmailChangeRequest;
import com.duebook.app.dto.UpdateBasicInfoRequest;
import com.duebook.app.dto.UpdateBasicInfoWithOtpRequest;
import com.duebook.app.dto.UserProfileDTO;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OtpService otpService;

    @Transactional
    public void requestOtpForEmailChange(Long userId, RequestOtpForEmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newEmail = request.getNewEmail().trim();

        // Check if new email is the same as current email
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new RuntimeException("New email must be different from current email");
        }

        // Check if new email is already in use by another user
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        // Generate and store OTP
        String otp = otpService.generateOtp();
        otpService.storeOtp(newEmail, otp);

        // Send OTP to the new email for primary email update
        log.info("OTP requested for email change from {} to {} for user ID: {}", user.getEmail(), newEmail, userId);
        otpService.sendPrimaryEmailUpdateOtp(newEmail, user.getName(), otp);
    }

    @Transactional
    public UserProfileDTO updateBasicInfoWithOtp(Long userId, UpdateBasicInfoWithOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate and update email if changed
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                String newEmail = request.getEmail().trim();

                // OTP verification required for email change
                if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                    throw new RuntimeException("OTP is required to change email address");
                }

                // Verify OTP against the new email
                if (!otpService.verifyOtp(newEmail, request.getOtp())) {
                    log.warn("OTP verification failed for email change to {} for user ID: {}", newEmail, userId);
                    throw new RuntimeException("Invalid or expired OTP");
                }

                // Check if new email is already in use
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already in use");
                }
                log.info("Email updated from {} to {} for user ID: {}", user.getEmail(), newEmail, userId);
                user.setEmail(newEmail);
            }
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        User savedUser = userRepository.save(user);
        log.info("User ID: {} profile updated with OTP verification", userId);
        return convertToDTO(savedUser);
    }

    public UserProfileDTO getUserProfile(Long userId) {
        log.debug("Fetching profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToDTO(user);
    }

    @Transactional
    public UserProfileDTO updateBasicInfo(Long userId, UpdateBasicInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Updating basic info for user ID: {}", userId);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        // Validate and update email if changed
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                String newEmail = request.getEmail().trim();

                // Check if new email is already in use
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already in use");
                }
                log.info("Email updated from {} to {} for user ID: {}", user.getEmail(), newEmail, userId);
                user.setEmail(newEmail);
            }
        } else {
            user.setEmail(request.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.info("User ID: {} basic info updated successfully", userId);
        return convertToDTO(savedUser);
    }

    private UserProfileDTO convertToDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        return dto;
    }
}

