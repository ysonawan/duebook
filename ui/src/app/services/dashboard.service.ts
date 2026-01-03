import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardMetricsDTO {
  // Customer Metrics
  totalCustomers: number;
  activeCustomers: number;
  totalShops: number;

  // Ledger Metrics
  totalDebit: number;
  totalCredit: number;
  netBalance: number;
  totalTransactions: number;
  averageTransactionValue: number;

  // Top 10 Customers (Highest Baki)
  topCustomers: TopCustomerDTO[];

  // Entry Type Distribution (Entry counts for last 30 days)
  entryTypeDistribution: EntryTypeDistributionDTO;

  // Transaction Trend (Last 30 days - daily breakdown)
  transactionTrend: DailyTransactionTrendDTO[];

  // Shop Distribution (Customer count per shop)
  shopDistribution: ShopDistributionDTO[];

  // Additional Useful Metrics
  averageCustomerBalance: number;
  overdueBakiCount: number;
  totalOverdueBaki: number;
  paymentHealthMetrics: PaymentHealthMetricsDTO;
}

export interface TopCustomerDTO {
  customerId: number;
  name: string;
  entityName: string;
  shopId: number;
  shopName: string;
  currentBalance: number;
}

export interface EntryTypeDistributionDTO {
  bakiCount: number;
  paidCount: number;
  bakiAmount: number;
  paidAmount: number;
}

export interface DailyTransactionTrendDTO {
  date: string;
  debitAmount: number;
  debitCount: number;
  creditAmount: number;
  creditCount: number;
}

export interface ShopDistributionDTO {
  shopId: number;
  shopName: string;
  customerCount: number;
  totalBalance: number;
}

export interface PaymentHealthMetricsDTO {
  collectionRate: number;  // Percentage of credit against total debit
  totalActiveCustomersWithBalance: number;
  largestOutstandingBalance: number;
  customersAboveAverageBalance: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = '/api/dashboard';

  constructor(private http: HttpClient) {}

  /**
   * Get comprehensive dashboard metrics
   */
  getDashboardMetrics(): Observable<DashboardMetricsDTO> {
    return this.http.get<DashboardMetricsDTO>(`${this.apiUrl}/metrics`);
  }

  /**
   * Get dashboard metrics filtered by shop ID
   */
  getDashboardMetricsByShop(shopId: number): Observable<DashboardMetricsDTO> {
    return this.http.get<DashboardMetricsDTO>(`${this.apiUrl}/metrics/shop/${shopId}`);
  }
}

