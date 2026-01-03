package com.duebook.app.service;

import com.duebook.app.dto.DashboardMetricsDTO;
import com.duebook.app.model.Customer;
import com.duebook.app.model.CustomerLedger;
import com.duebook.app.model.Shop;
import com.duebook.app.repository.CustomerRepository;
import com.duebook.app.repository.CustomerLedgerRepository;
import com.duebook.app.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final ShopRepository shopRepository;

    /**
     * Get comprehensive dashboard metrics for the authenticated user
     * If shopId is provided, metrics will be filtered for that specific shop only
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDTO getDashboardMetrics(Long userId, Long shopId) {
        log.info("Computing dashboard metrics for user ID: {} and shop ID: {}", userId, shopId);

        // Fetch all required data with proper filtering for user's shops
        List<Shop> userShops = shopRepository.findByUserId(userId);
        List<Long> shopIds = userShops.stream().map(Shop::getId).toList();

        // If shopId is provided, filter to only that shop (if it belongs to the user)
        if (shopId != null) {
            if (!shopIds.contains(shopId)) {
                log.warn("User {} does not have access to shop {}", userId, shopId);
                return buildEmptyMetrics();
            }
            shopIds = List.of(shopId);
            userShops = userShops.stream().filter(s -> s.getId().equals(shopId)).toList();
        }

        if (shopIds.isEmpty()) {
            log.warn("User {} has no shops", userId);
            return buildEmptyMetrics();
        }

        List<Customer> customers = customerRepository.findByShopIdIn(shopIds);
        List<CustomerLedger> ledgerEntries = customerLedgerRepository.findByShopIdIn(shopIds);

        DashboardMetricsDTO metrics = DashboardMetricsDTO.builder().build();

        // Calculate Customer Metrics
        calculateCustomerMetrics(metrics, customers, userShops);

        // Calculate Ledger Metrics
        calculateLedgerMetrics(metrics, ledgerEntries);

        // Calculate Top 10 Customers
        calculateTopCustomers(metrics, customers, userShops);

        // Calculate Entry Type Distribution (Last 30 days)
        calculateEntryTypeDistribution(metrics, ledgerEntries);

        // Calculate Transaction Trend (Last 30 days)
        calculateTransactionTrend(metrics, ledgerEntries);

        // Calculate Shop Distribution
        calculateShopDistribution(metrics, customers, userShops);

        // Calculate Additional Metrics
        calculateAdditionalMetrics(metrics, customers);

        log.info("Dashboard metrics computed successfully for user ID: {} and shop ID: {}", userId, shopId);
        return metrics;
    }

    private DashboardMetricsDTO buildEmptyMetrics() {
        return DashboardMetricsDTO.builder()
                .totalCustomers(0L)
                .activeCustomers(0L)
                .totalShops(0L)
                .totalDebit(0.0)
                .totalCredit(0.0)
                .netBalance(0.0)
                .totalTransactions(0L)
                .averageTransactionValue(0.0)
                .topCustomers(Collections.emptyList())
                .transactionTrend(Collections.emptyList())
                .shopDistribution(Collections.emptyList())
                .entryTypeDistribution(DashboardMetricsDTO.EntryTypeDistributionDTO.builder()
                        .bakiCount(0L)
                        .paidCount(0L)
                        .bakiAmount(0.0)
                        .paidAmount(0.0)
                        .build())
                .build();
    }


    private void calculateCustomerMetrics(DashboardMetricsDTO metrics, List<Customer> customers, List<Shop> userShops) {
        metrics.setTotalCustomers((long) customers.size());
        metrics.setActiveCustomers(customers.stream().filter(c -> c.getIsActive() != null && c.getIsActive()).count());
        metrics.setTotalShops(userShops.stream().filter(s -> s.getIsActive() != null && s.getIsActive()).count());
    }

    private void calculateLedgerMetrics(DashboardMetricsDTO metrics, List<CustomerLedger> ledgerEntries) {
        // Get all entry IDs that have been reversed
        Set<Long> reversedEntryIds = ledgerEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.REVERSAL)
                .map(e -> e.getReferenceEntry().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Filter out reversed entries and reversals themselves
        List<CustomerLedger> effectiveEntries = ledgerEntries.stream()
                .filter(e -> !reversedEntryIds.contains(e.getId()) && e.getEntryType() != CustomerLedger.LedgerEntryType.REVERSAL)
                .toList();

        Double totalDebit = effectiveEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                .mapToDouble(CustomerLedger::getAmount)
                .sum();

        Double totalCredit = effectiveEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                .mapToDouble(CustomerLedger::getAmount)
                .sum();

        Double netBalance = totalDebit - totalCredit;

        metrics.setTotalDebit(totalDebit);
        metrics.setTotalCredit(totalCredit);
        metrics.setNetBalance(netBalance);
        metrics.setTotalTransactions((long) effectiveEntries.size());

        if (!effectiveEntries.isEmpty()) {
            metrics.setAverageTransactionValue((totalDebit + totalCredit) / effectiveEntries.size());
        } else {
            metrics.setAverageTransactionValue(0.0);
        }
    }

    private void calculateTopCustomers(DashboardMetricsDTO metrics, List<Customer> customers, List<Shop> userShops) {
        Map<Long, String> shopMap = userShops.stream().collect(Collectors.toMap(Shop::getId, Shop::getName));

        List<DashboardMetricsDTO.TopCustomerDTO> topCustomers = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .sorted((a, b) -> Double.compare(b.getCurrentBalance(), a.getCurrentBalance()))
                .limit(10)
                .map(c -> DashboardMetricsDTO.TopCustomerDTO.builder()
                        .customerId(c.getId())
                        .name(c.getName())
                        .entityName(c.getEntityName())
                        .shopId(c.getShop().getId())
                        .shopName(shopMap.getOrDefault(c.getShop().getId(), "N/A"))
                        .currentBalance(c.getCurrentBalance())
                        .build())
                .toList();

        metrics.setTopCustomers(topCustomers);
    }

    private void calculateEntryTypeDistribution(DashboardMetricsDTO metrics, List<CustomerLedger> ledgerEntries) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Get all entry IDs that have been reversed
        Set<Long> reversedEntryIds = ledgerEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.REVERSAL)
                .map(e -> e.getReferenceEntry().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CustomerLedger> last30DaysEntries = ledgerEntries.stream()
                .filter(e -> !reversedEntryIds.contains(e.getId()) && e.getEntryType() != CustomerLedger.LedgerEntryType.REVERSAL)
                .filter(e -> e.getEntryDate().isAfter(thirtyDaysAgo) || e.getEntryDate().isEqual(thirtyDaysAgo))
                .toList();

        Long bakiCount = last30DaysEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                .count();

        Long paidCount = last30DaysEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                .count();

        Double bakiAmount = last30DaysEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                .mapToDouble(CustomerLedger::getAmount)
                .sum();

        Double paidAmount = last30DaysEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                .mapToDouble(CustomerLedger::getAmount)
                .sum();

        DashboardMetricsDTO.EntryTypeDistributionDTO distribution = DashboardMetricsDTO.EntryTypeDistributionDTO.builder()
                .bakiCount(bakiCount)
                .paidCount(paidCount)
                .bakiAmount(bakiAmount)
                .paidAmount(paidAmount)
                .build();

        metrics.setEntryTypeDistribution(distribution);
    }

    private void calculateTransactionTrend(DashboardMetricsDTO metrics, List<CustomerLedger> ledgerEntries) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Get all entry IDs that have been reversed
        Set<Long> reversedEntryIds = ledgerEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.REVERSAL)
                .map(e -> e.getReferenceEntry().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CustomerLedger> last30DaysEntries = ledgerEntries.stream()
                .filter(e -> !reversedEntryIds.contains(e.getId()) && e.getEntryType() != CustomerLedger.LedgerEntryType.REVERSAL)
                .filter(e -> e.getEntryDate().isAfter(thirtyDaysAgo) || e.getEntryDate().isEqual(thirtyDaysAgo))
                .toList();

        Map<LocalDate, List<CustomerLedger>> groupedByDate = last30DaysEntries.stream()
                .collect(Collectors.groupingBy(CustomerLedger::getEntryDate));

        List<DashboardMetricsDTO.DailyTransactionTrendDTO> trends = new ArrayList<>();

        for (long i = 0; i <= 30; i++) {
            LocalDate date = LocalDate.now().minusDays(30 - i);
            List<CustomerLedger> dayEntries = groupedByDate.getOrDefault(date, Collections.emptyList());

            double debitAmount = dayEntries.stream()
                    .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                    .mapToDouble(CustomerLedger::getAmount)
                    .sum();

            long debitCount = dayEntries.stream()
                    .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                    .count();

            double creditAmount = dayEntries.stream()
                    .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                    .mapToDouble(CustomerLedger::getAmount)
                    .sum();

            long creditCount = dayEntries.stream()
                    .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                    .count();

            if (debitAmount > 0 || creditAmount > 0 || debitCount > 0 || creditCount > 0) {
                trends.add(DashboardMetricsDTO.DailyTransactionTrendDTO.builder()
                        .date(date.toString())
                        .debitAmount(debitAmount)
                        .debitCount(debitCount)
                        .creditAmount(creditAmount)
                        .creditCount(creditCount)
                        .build());
            }
        }

        metrics.setTransactionTrend(trends);
    }

    private void calculateShopDistribution(DashboardMetricsDTO metrics, List<Customer> customers, List<Shop> userShops) {
        Map<Long, String> shopMap = userShops.stream().collect(Collectors.toMap(Shop::getId, Shop::getName));

        Map<Long, List<Customer>> customersByShop = customers.stream()
                .collect(Collectors.groupingBy(c -> c.getShop().getId()));

        List<DashboardMetricsDTO.ShopDistributionDTO> distribution = customersByShop.entrySet().stream()
                .map(entry -> DashboardMetricsDTO.ShopDistributionDTO.builder()
                        .shopId(entry.getKey())
                        .shopName(shopMap.getOrDefault(entry.getKey(), "N/A"))
                        .customerCount((long) entry.getValue().size())
                        .totalBalance(entry.getValue().stream()
                                .mapToDouble(c -> c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0)
                                .sum())
                        .build())
                .collect(Collectors.toList());

        metrics.setShopDistribution(distribution);
    }

    private void calculateAdditionalMetrics(DashboardMetricsDTO metrics, List<Customer> customers) {
        // Average customer balance
        double averageBalance = customers.stream()
                .mapToDouble(c -> c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0)
                .average()
                .orElse(0.0);
        metrics.setAverageCustomerBalance(averageBalance);

        // Overdue Baki count and total (customers with balance > 0 can be considered as having pending dues)
        long overdueBakiCount = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .count();

        Double totalOverdueBaki = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .mapToDouble(Customer::getCurrentBalance)
                .sum();

        metrics.setOverdueBakiCount(overdueBakiCount);
        metrics.setTotalOverdueBaki(totalOverdueBaki);

        // Payment Health Metrics
        double collectionRate = 0.0;
        if (metrics.getTotalDebit() != null && metrics.getTotalDebit() > 0) {
            collectionRate = (metrics.getTotalCredit() / metrics.getTotalDebit()) * 100;
        }

        long activeCustomersWithBalance = customers.stream()
                .filter(c -> c.getIsActive() != null && c.getIsActive() && c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .count();

        Double largestOutstanding = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .mapToDouble(Customer::getCurrentBalance)
                .max()
                .orElse(0.0);

        long customersAboveAverage = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > averageBalance)
                .count();

        DashboardMetricsDTO.PaymentHealthMetricsDTO healthMetrics = DashboardMetricsDTO.PaymentHealthMetricsDTO.builder()
                .collectionRate(collectionRate)
                .totalActiveCustomersWithBalance(activeCustomersWithBalance)
                .largestOutstandingBalance(largestOutstanding)
                .customersAboveAverageBalance(customersAboveAverage)
                .build();

        metrics.setPaymentHealthMetrics(healthMetrics);
    }
}

