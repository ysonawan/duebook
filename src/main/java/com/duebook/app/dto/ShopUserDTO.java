package com.duebook.app.dto;

import com.duebook.app.model.ShopUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopUserDTO {
    private Long id;

    @NotNull(message = "Shop ID is required")
    private Long shopId;

    @NotBlank(message = "User phone number is required")
    @Size(min = 10, max = 10, message = "User phone number must be 10 digits")
    private String userPhone;

    private Long userId;

    private String userName;

    private String userEmail;

    @NotNull(message = "Role is required")
    private ShopUser.ShopUserRole role;

    private ShopUser.ShopUserStatus status;

    private LocalDateTime joinedAt;
}

