package com.duebook.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSecondaryEmailsWithOtpRequest {

    @NotNull(message = "Secondary emails list is required")
    private List<String> secondaryEmails;

    private String otp;
}

