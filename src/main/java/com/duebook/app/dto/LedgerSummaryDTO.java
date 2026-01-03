package com.duebook.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerSummaryDTO {
    private Double totalDebit;
    private Double totalCredit;
    private Double netBalance;
    private Long totalEntries;
}

