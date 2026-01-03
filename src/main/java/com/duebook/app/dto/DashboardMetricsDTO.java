package com.duebook.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsDTO {
    // Customer Metrics
    private Long totalCustomers;
    private Long activeCustomers;
    private Long totalShops;

    // Ledger Metrics
    private Double totalDebit;
    private Double totalCredit;
    private Double netBalance;
    private Long totalTransactions;
    private Double averageTransactionValue;

    // Top 10 Customers (Highest Baki)
    private List<TopCustomerDTO> topCustomers;

    // Entry Type Distribution (Entry counts for last 30 days)
    private EntryTypeDistributionDTO entryTypeDistribution;

    // Transaction Trend (Last 30 days - daily breakdown)
    private List<DailyTransactionTrendDTO> transactionTrend;

    // Shop Distribution (Customer count per shop)
    private List<ShopDistributionDTO> shopDistribution;

    // Additional Useful Metrics
    private Double averageCustomerBalance;
    private Long overdueBakiCount;
    private Double totalOverdueBaki;
    private PaymentHealthMetricsDTO paymentHealthMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCustomerDTO {
        private Long customerId;
        private String name;
        private String entityName;
        private Long shopId;
        private String shopName;
        private Double currentBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntryTypeDistributionDTO {
        private Long bakiCount;
        private Long paidCount;
        private Double bakiAmount;
        private Double paidAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTransactionTrendDTO {
        private String date;
        private Double debitAmount;
        private Long debitCount;
        private Double creditAmount;
        private Long creditCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopDistributionDTO {
        private Long shopId;
        private String shopName;
        private Long customerCount;
        private Double totalBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentHealthMetricsDTO {
        private Double collectionRate;  // Percentage of credit against total debit
        private Long totalActiveCustomersWithBalance;
        private Double largestOutstandingBalance;
        private Long customersAboveAverageBalance;
    }
}

