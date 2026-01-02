package com.duebook.app.service;

import com.duebook.app.dto.RequestOtpForEmailChangeRequest;
import com.duebook.app.dto.UpdateBasicInfoRequest;
import com.duebook.app.dto.UpdateBasicInfoWithOtpRequest;
import com.duebook.app.dto.UpdateSecondaryEmailsRequest;
import com.duebook.app.dto.UpdateSecondaryEmailsWithOtpRequest;
import com.duebook.app.dto.UserProfileDTO;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        otpService.sendPrimaryEmailUpdateOtp(newEmail, user.getName(), otp);
    }

    @Transactional
    public UserProfileDTO updateBasicInfoWithOtp(Long userId, UpdateBasicInfoWithOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean emailChanged = false;

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
                    throw new RuntimeException("Invalid or expired OTP");
                }

                // Check if new email is already in use
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already in use");
                }

                // Check if new email is part of user's own secondary emails
                if (user.getSecondaryEmails() != null && !user.getSecondaryEmails().trim().isEmpty()) {
                    List<String> secondaryEmails = Arrays.stream(user.getSecondaryEmails().split(","))
                            .map(String::trim)
                            .filter(email -> !email.isEmpty())
                            .collect(Collectors.toList());

                    for (String secondaryEmail : secondaryEmails) {
                        if (secondaryEmail.equalsIgnoreCase(newEmail)) {
                            throw new RuntimeException("Cannot use your secondary email as primary email");
                        }
                    }
                }

                user.setEmail(newEmail);
                emailChanged = true;
            }
        }

        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Transactional
    public void requestOtpForSecondaryEmailChange(Long userId, List<String> newSecondaryEmails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only accept one email at a time
        if (newSecondaryEmails == null || newSecondaryEmails.isEmpty()) {
            throw new RuntimeException("No email provided to verify");
        }

        if (newSecondaryEmails.size() > 1) {
            throw new RuntimeException("Please add one email at a time");
        }

        String newEmail = newSecondaryEmails.get(0).trim();

        // Validate email is not already in secondary emails
        if (user.getSecondaryEmails() != null && !user.getSecondaryEmails().trim().isEmpty()) {
            List<String> currentSecondaryEmails = Arrays.stream(user.getSecondaryEmails().split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .collect(Collectors.toList());

            if (currentSecondaryEmails.stream().anyMatch(e -> e.equalsIgnoreCase(newEmail))) {
                throw new RuntimeException("This email is already added");
            }
        }

        // Validate email is not the primary email
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Secondary email cannot be the same as primary email");
        }

        // Generate OTP and send to the new email for secondary email addition
        String otp = otpService.generateOtp();
        otpService.storeOtp(newEmail, otp);
        otpService.sendSecondaryEmailOtp(newEmail, user.getName(), otp);
    }

    @Transactional
    public UserProfileDTO updateSecondaryEmailsWithOtp(Long userId, UpdateSecondaryEmailsWithOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get current secondary emails
        final List<String> currentSecondaryEmails;
        if (user.getSecondaryEmails() != null && !user.getSecondaryEmails().trim().isEmpty()) {
            currentSecondaryEmails = Arrays.stream(user.getSecondaryEmails().split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .collect(Collectors.toList());
        } else {
            currentSecondaryEmails = new ArrayList<>();
        }

        // Find newly added emails
        List<String> newlyAddedEmails = request.getSecondaryEmails().stream()
                .filter(email -> !currentSecondaryEmails.contains(email))
                .collect(Collectors.toList());

        // If there are newly added emails, OTP verification is required
        if (!newlyAddedEmails.isEmpty()) {
            // Only allow one email to be added at a time
            if (newlyAddedEmails.size() > 1) {
                throw new RuntimeException("Please add one email at a time");
            }

            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                throw new RuntimeException("OTP is required to add new secondary email");
            }

            // Verify OTP against the newly added email
            String newEmail = newlyAddedEmails.get(0);
            if (!otpService.verifyOtp(newEmail, request.getOtp())) {
                throw new RuntimeException("Invalid or expired OTP");
            }
        }

        // Validate that secondary emails don't exceed 10
        if (request.getSecondaryEmails() != null && request.getSecondaryEmails().size() > 10) {
            throw new RuntimeException("Maximum 10 secondary emails allowed");
        }

        // Validate that secondary emails don't include the primary email
        if (request.getSecondaryEmails() != null) {
            for (String email : request.getSecondaryEmails()) {
                if (email != null && email.trim().equalsIgnoreCase(user.getEmail())) {
                    throw new RuntimeException("Secondary email cannot be the same as primary email");
                }
            }
        }

        // Convert list to comma-separated string
        String secondaryEmailsStr = null;
        if (request.getSecondaryEmails() != null && !request.getSecondaryEmails().isEmpty()) {
            // Filter out empty strings and trim
            List<String> validEmails = request.getSecondaryEmails().stream()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            if (!validEmails.isEmpty()) {
                secondaryEmailsStr = String.join(",", validEmails);
            }
        }

        user.setSecondaryEmails(secondaryEmailsStr);
        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToDTO(user);
    }

    @Transactional
    public UserProfileDTO updateSecondaryEmails(Long userId, UpdateSecondaryEmailsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate that secondary emails don't exceed 10
        if (request.getSecondaryEmails() != null && request.getSecondaryEmails().size() > 10) {
            throw new RuntimeException("Maximum 10 secondary emails allowed");
        }

        // Validate that secondary emails don't include the primary email
        if (request.getSecondaryEmails() != null) {
            for (String email : request.getSecondaryEmails()) {
                if (email != null && email.trim().equalsIgnoreCase(user.getEmail())) {
                    throw new RuntimeException("Secondary email cannot be the same as primary email");
                }
            }
        }

        // Convert list to comma-separated string
        String secondaryEmailsStr = null;
        if (request.getSecondaryEmails() != null && !request.getSecondaryEmails().isEmpty()) {
            // Filter out empty strings and trim
            List<String> validEmails = request.getSecondaryEmails().stream()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            if (!validEmails.isEmpty()) {
                secondaryEmailsStr = String.join(",", validEmails);
            }
        }

        user.setSecondaryEmails(secondaryEmailsStr);
        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    @Transactional
    public UserProfileDTO updateBasicInfo(Long userId, UpdateBasicInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate and update email if changed
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
                String newEmail = request.getEmail().trim();

                // Check if new email is already in use
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already in use");
                }

                // Check if new email is part of user's own secondary emails
                if (user.getSecondaryEmails() != null && !user.getSecondaryEmails().trim().isEmpty()) {
                    List<String> secondaryEmails = Arrays.stream(user.getSecondaryEmails().split(","))
                            .map(String::trim)
                            .filter(email -> !email.isEmpty())
                            .collect(Collectors.toList());

                    for (String secondaryEmail : secondaryEmails) {
                        if (secondaryEmail.equalsIgnoreCase(newEmail)) {
                            throw new RuntimeException("Cannot use your secondary email as primary email");
                        }
                    }
                }

                user.setEmail(newEmail);
            }
        }

        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    private UserProfileDTO convertToDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());

        // Convert comma-separated string to list
        List<String> secondaryEmails = new ArrayList<>();
        if (user.getSecondaryEmails() != null && !user.getSecondaryEmails().trim().isEmpty()) {
            secondaryEmails = Arrays.stream(user.getSecondaryEmails().split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .collect(Collectors.toList());
        }
        dto.setSecondaryEmails(secondaryEmails);

        return dto;
    }
}

