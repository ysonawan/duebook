package com.duebook.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSummaryDTO {
    private Long totalCustomers;
    private Long activeCustomers;
    private Double totalOpeningBalance;
    private Double totalCurrentBalance;
}

