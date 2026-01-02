package com.duebook.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecondaryEmailsRequest {
    @Size(max = 10, message = "Maximum 10 secondary emails allowed")
    private List<@Email(message = "Invalid email format") String> secondaryEmails;
}

