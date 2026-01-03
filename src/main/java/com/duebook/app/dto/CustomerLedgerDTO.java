package com.duebook.app.dto;

import com.duebook.app.model.CustomerLedger;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLedgerDTO {
    private Long id;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private Long shopId;

    @NotNull(message = "Entry type is required")
    private CustomerLedger.LedgerEntryType entryType;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount should be greater than 0")
    private Double amount;

    private Double balanceAfter;

    private Long referenceEntryId;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    private LocalDateTime createdAt;

    private UserDTO createdByUser;

    private CustomerDTO customer;
}

