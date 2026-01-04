import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardService, DashboardMetricsDTO, TopCustomerDTO, ShopDistributionDTO, DailyTransactionTrendDTO, EntryTypeDistributionDTO, PaymentHealthMetricsDTO } from '../../services/dashboard.service';
import { ShopService } from '../../services/shop.service';
import { Shop } from '../../models/shop.model';


@Component({
  selector: 'app-dashboard-duebook',
  templateUrl: './dashboard-duebook.component.html',
  styleUrls: ['./dashboard-duebook.component.css'],
  standalone: false
})
export class DashboardDuebookComponent implements OnInit {
  // Loading state
  loading = true;

  // Shop Filter
  shops: Shop[] = [];
  selectedShopId: number = 0;

  // Customer Metrics
  totalCustomers: number = 0;
  activeCustomers: number = 0;
  totalShops: number = 0;

  // Ledger Metrics
  totalDebit: number = 0;
  totalCredit: number = 0;
  netBalance: number = 0;
  totalTransactions: number = 0;
  averageTransactionValue: number = 0;

  // Additional Metrics
  averageCustomerBalance: number = 0;
  overdueBakiCount: number = 0;
  totalOverdueBaki: number = 0;
  paymentHealthMetrics: PaymentHealthMetricsDTO | null = null;

  // Chart Options
  ledgerTrendChartOptions: any = {};
  shopDistributionChartOptions: any = {};
  entryTypeChartOptions: any = {};

  // Chart Visibility
  hasLedgerChartData = false;
  hasShopChartData = false;
  hasEntryTypeData = false;

  // Top Customers
  topCustomers: TopCustomerDTO[] = [];
  maxTopCustomerBalance: number = 0;

  // Chart data
  shopDistribution: ShopDistributionDTO[] = [];
  transactionTrend: DailyTransactionTrendDTO[] = [];
  entryTypeDistribution: EntryTypeDistributionDTO | null = null;

  constructor(
    private dashboardService: DashboardService,
    private shopService: ShopService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadShops();
  }

  loadShops(): void {
    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops;
        this.loadDashboardMetrics();
      },
      error: (error) => {
        console.error('Error loading shops:', error);
        this.loading = false;
      }
    });
  }

  onShopChange(): void {
    this.loadDashboardMetrics();
  }

  viewLedger(customerId: number, shopId: number | undefined): void {
    this.router.navigate(['/ledger'], { queryParams: { customerId: customerId, shopId: shopId } });
  }

  loadDashboardMetrics(): void {
    this.loading = true;

    const metricsCall = Number(this.selectedShopId) === 0
      ? this.dashboardService.getDashboardMetrics()
      : this.dashboardService.getDashboardMetricsByShop(this.selectedShopId);

    metricsCall.subscribe({
      next: (metrics: DashboardMetricsDTO) => {
        // Set all metrics from backend
        this.totalCustomers = metrics.totalCustomers;
        this.activeCustomers = metrics.activeCustomers;
        this.totalShops = metrics.totalShops;

        this.totalDebit = metrics.totalDebit;
        this.totalCredit = metrics.totalCredit;
        this.netBalance = metrics.netBalance;
        this.totalTransactions = metrics.totalTransactions;
        this.averageTransactionValue = metrics.averageTransactionValue;

        this.averageCustomerBalance = metrics.averageCustomerBalance;
        this.overdueBakiCount = metrics.overdueBakiCount;
        this.totalOverdueBaki = metrics.totalOverdueBaki;
        this.paymentHealthMetrics = metrics.paymentHealthMetrics;

        // Set top customers
        this.topCustomers = metrics.topCustomers;
        this.maxTopCustomerBalance = this.topCustomers.length > 0 ? this.topCustomers[0].currentBalance : 1;

        // Set chart data
        this.transactionTrend = metrics.transactionTrend;
        this.shopDistribution = metrics.shopDistribution;
        this.entryTypeDistribution = metrics.entryTypeDistribution;

        // Prepare charts
        this.prepareLedgerTrendChart();
        this.prepareShopDistributionChart();
        this.prepareEntryTypeChart();

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard metrics:', err);
        this.loading = false;
      }
    });
  }

  prepareLedgerTrendChart(): void {
    // Data is already computed on backend, just format for chart
    const dates = this.transactionTrend.map(t => t.date);
    const debits = this.transactionTrend.map(t => t.debitAmount);
    const credits = this.transactionTrend.map(t => t.creditAmount);

    this.hasLedgerChartData = dates.length > 0;

    const isMobile = window.innerWidth < 640;

    this.ledgerTrendChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any) => {
          if (!Array.isArray(params)) params = [params];
          return params.map((p: any) => `${p.seriesName}: ₹${p.value.toLocaleString('en-IN')}`).join('<br/>');
        }
      },
      legend: {
        data: ['Debit (बाकी)', 'Credit (जमा)'],
        textStyle: { fontSize: isMobile ? 10 : 11 }
      },
      grid: {
        left: isMobile ? 40 : 50,
        right: isMobile ? 10 : 20,
        top: 40,
        bottom: isMobile ? 50 : 60,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          rotate: isMobile ? 45 : 30,
          fontSize: isMobile ? 9 : 10
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: { fontSize: isMobile ? 10 : 11 }
      },
      series: [
        {
          name: 'Debit (बाकी)',
          data: debits,
          type: 'line',
          smooth: true,
          itemStyle: { color: '#FF6B6B' },
          areaStyle: { color: 'rgba(255, 107, 107, 0.2)' }
        },
        {
          name: 'Credit (जमा)',
          data: credits,
          type: 'line',
          smooth: true,
          itemStyle: { color: '#51CF66' },
          areaStyle: { color: 'rgba(81, 207, 102, 0.2)' }
        }
      ]
    };
  }

  prepareShopDistributionChart(): void {
    // Data is already computed on backend
    const chartData = this.shopDistribution.map((shop, index) => ({
      name: shop.shopName,
      value: shop.customerCount,
      itemStyle: { color: this.getColorForIndex(index) }
    }));

    this.hasShopChartData = chartData.length > 0;

    const isMobile = window.innerWidth < 640;

    this.shopDistributionChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          if (!Array.isArray(params)) params = [params];
          return params.map((p: any) => `${p.name}: ${p.value} customers`).join('<br/>');
        }
      },
      grid: {
        left: isMobile ? 40 : 50,
        right: isMobile ? 10 : 20,
        top: 20,
        bottom: isMobile ? 40 : 50,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: chartData.map(d => d.name),
        axisLabel: {
          rotate: isMobile ? 45 : 0,
          fontSize: isMobile ? 10 : 11
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: { fontSize: isMobile ? 10 : 11 }
      },
      series: [
        {
          data: chartData.map(d => ({ value: d.value, itemStyle: d.itemStyle })),
          type: 'bar',
          itemStyle: { borderRadius: [8, 8, 0, 0] },
          emphasis: { focus: 'series' }
        }
      ]
    };
  }

  prepareEntryTypeChart(): void {
    // Data is already computed on backend
    const data = [
      { name: 'Debit (बाकी)', value: this.entryTypeDistribution?.bakiCount || 0, itemStyle: { color: '#FF6B6B' } },
      { name: 'Credit (जमा)', value: this.entryTypeDistribution?.paidCount || 0, itemStyle: { color: '#51CF66' } }
    ].filter(d => d.value > 0);

    this.hasEntryTypeData = data.length > 0;

    const isMobile = window.innerWidth < 640;

    this.entryTypeChartOptions = {
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          return `${params.name}: ${params.value} entries`;
        },
        confine: true
      },
      legend: {
        orient: isMobile ? 'horizontal' : 'vertical',
        bottom: isMobile ? 0 : undefined,
        right: isMobile ? undefined : 10,
        top: isMobile ? undefined : 'center',
        left: isMobile ? 'center' : undefined,
        textStyle: { fontSize: isMobile ? 10 : 11 }
      },
      series: [
        {
          name: 'Entries',
          type: 'pie',
          radius: isMobile ? '50%' : '65%',
          center: isMobile ? ['50%', '42%'] : ['40%', '50%'],
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: { show: false }
        }
      ]
    };
  }

  getColorForIndex(index: number): string {
    const colors = [
      '#FF8C00', '#0099CC', '#99CC00', '#9900CC', '#00CC99',
      '#FF00FF', '#808080', '#00FF00', '#FF0000', '#00FFFF',
      '#FF6600', '#9900FF', '#0000FF', '#00FF99', '#CC00FF',
      '#CC9900', '#FF0099', '#99FF00', '#0099FF', '#CC0099'
    ];
    return colors[index % colors.length];
  }

  formatCurrency(value: number | undefined): string {
    if (!value || value === 0) {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
      }).format(0);
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  // Navigation Methods
  navigateToCustomers(): void {
    this.router.navigate(['/customers']);
  }

  navigateToLedger(): void {
    this.router.navigate(['/ledger']);
  }

  navigateToShops(): void {
    this.router.navigate(['/shops']);
  }
}

