package com.duebook.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String name;

    @Size(min = 2, max = 255, message = "Entity name must be between 2 and 255 characters")
    private String entityName;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 10, message = "Phone number must be 10 digits")
    private String phone;

    @NotNull(message = "Opening balance is required")
    @Min(value = 0, message = "Opening balance cannot be negative")
    private Double openingBalance;

    @NotNull(message = "Current balance is required")
    private Double currentBalance;

    @NotNull(message = "Shop is required")
    private Long shopId;

    private ShopDTO shop;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

