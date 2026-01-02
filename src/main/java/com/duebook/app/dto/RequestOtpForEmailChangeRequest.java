package com.duebook.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestOtpForEmailChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "Email should be valid")
    private String newEmail;
}

