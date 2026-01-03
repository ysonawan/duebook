import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { LedgerService } from '../../services/ledger.service';
import { ShopService } from '../../services/shop.service';
import { Customer } from '../../models/customer.model';
import { CustomerLedger, LedgerEntryType } from '../../models/ledger.model';
import { Shop } from '../../models/shop.model';

@Component({
  selector: 'app-dashboard-duebook',
  templateUrl: './dashboard-duebook.component.html',
  styleUrls: ['./dashboard-duebook.component.css'],
  standalone: false
})
export class DashboardDuebookComponent implements OnInit {
  // Data
  customers: Customer[] = [];
  ledgerEntries: CustomerLedger[] = [];
  shops: Shop[] = [];
  loading = true;

  // Customer Metrics
  totalCustomers: number = 0;
  activeCustomers: number = 0;
  totalCustomerBalance: number = 0;

  // Ledger Metrics
  totalDebit: number = 0;
  totalCredit: number = 0;
  netBalance: number = 0;
  totalTransactions: number = 0;
  averageTransactionValue: number = 0;

  // Shop Metrics
  totalShops: number = 0;

  // Chart Options
  customerBalanceChartOptions: any = {};
  ledgerTrendChartOptions: any = {};
  shopDistributionChartOptions: any = {};
  entryTypeChartOptions: any = {};

  // Chart Visibility
  hasCustomerChartData = false;
  hasLedgerChartData = false;
  hasShopChartData = false;
  hasEntryTypeData = false;

  // Top Customers
  topCustomers: Customer[] = [];
  maxTopCustomerBalance: number = 0;

  constructor(
    private customerService: CustomerService,
    private ledgerService: LedgerService,
    private shopService: ShopService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.loading = true;

    this.customerService.getAllCustomers().subscribe({
      next: (customers) => {
        this.customers = customers;
        this.calculateCustomerMetrics();
        this.prepareCustomerBalanceChart();
        this.prepareTopCustomers();
      },
      error: (err) => {
        console.error('Error loading customers:', err);
      }
    });

    this.ledgerService.getAllLedgerEntries().subscribe({
      next: (entries) => {
        this.ledgerEntries = entries;
        this.calculateLedgerMetrics();
        this.prepareLedgerTrendChart();
        this.prepareEntryTypeChart();
      },
      error: (err) => {
        console.error('Error loading ledger:', err);
      }
    });

    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops.filter(s => s.isActive !== false);
        this.totalShops = this.shops.length;
        this.prepareShopDistributionChart();
      },
      error: (err) => {
        console.error('Error loading shops:', err);
      }
    });
    this.loading = false;
  }
  calculateCustomerMetrics(): void {
    this.totalCustomers = this.customers.length;
    this.activeCustomers = this.customers.filter(c => c.isActive !== false).length;
    this.totalCustomerBalance = this.customers.reduce((sum, c) => sum + (c.currentBalance || 0), 0);
  }

  calculateLedgerMetrics(): void {
    // Get all entry IDs that have been reversed
    const reversedEntryIds: number[] = this.ledgerEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    // Filter out reversed entries and reversals themselves
    const effectiveEntries = this.ledgerEntries.filter(e =>
      !reversedEntryIds.includes(e.id!) && e.entryType !== LedgerEntryType.REVERSAL
    );

    this.totalDebit = effectiveEntries
      .filter(e => e.entryType === LedgerEntryType.BAKI)
      .reduce((sum, e) => sum + e.amount, 0);

    this.totalCredit = effectiveEntries
      .filter(e => e.entryType === LedgerEntryType.PAID)
      .reduce((sum, e) => sum + e.amount, 0);

    this.netBalance = this.totalDebit - this.totalCredit;
    this.totalTransactions = effectiveEntries.length;

    if (effectiveEntries.length > 0) {
      this.averageTransactionValue = (this.totalDebit + this.totalCredit) / effectiveEntries.length;
    }
  }

  prepareCustomerBalanceChart(): void {
    // Top 5 customers by balance
    const topCustomers = [...this.customers]
      .filter(c => c.currentBalance !== 0)
      .sort((a, b) => Math.abs(b.currentBalance) - Math.abs(a.currentBalance))
      .slice(0, 5);

    if (topCustomers.length === 0) {
      this.hasCustomerChartData = false;
      return;
    }

    const data = topCustomers.map((customer, index) => ({
      name: customer.name,
      value: Math.abs(customer.currentBalance),
      itemStyle: { color: this.getColorForIndex(index) }
    }));

    this.hasCustomerChartData = true;
    const isMobile = window.innerWidth < 640;

    this.customerBalanceChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          if (!Array.isArray(params)) params = [params];
          return params.map((p: any) => `${p.name}: ₹${p.value.toLocaleString('en-IN')}`).join('<br/>');
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
        data: data.map(d => d.name),
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
          data: data.map(d => ({ value: d.value, itemStyle: d.itemStyle })),
          type: 'bar',
          itemStyle: { borderRadius: [8, 8, 0, 0] },
          emphasis: { focus: 'series' }
        }
      ]
    };
  }

  prepareLedgerTrendChart(): void {
    // Get daily debit and credit totals
    const dailyData = new Map<string, { debit: number; credit: number }>();

    const reversedEntryIds: number[] = this.ledgerEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    const effectiveEntries = this.ledgerEntries.filter(e =>
      !reversedEntryIds.includes(e.id!) && e.entryType !== LedgerEntryType.REVERSAL
    );

    effectiveEntries.forEach(entry => {
      const date = new Date(entry.entryDate).toLocaleDateString();
      if (!dailyData.has(date)) {
        dailyData.set(date, { debit: 0, credit: 0 });
      }
      const data = dailyData.get(date)!;
      if (entry.entryType === LedgerEntryType.BAKI) {
        data.debit += entry.amount;
      } else {
        data.credit += entry.amount;
      }
    });

    const dates = Array.from(dailyData.keys()).sort().slice(-30); // Last 30 days
    const debits = dates.map(date => dailyData.get(date)!.debit);
    const credits = dates.map(date => dailyData.get(date)!.credit);

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
        data: ['Debit (Baki)', 'Credit (Paid)'],
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
          name: 'Debit (Baki)',
          data: debits,
          type: 'line',
          smooth: true,
          itemStyle: { color: '#FF6B6B' },
          areaStyle: { color: 'rgba(255, 107, 107, 0.2)' }
        },
        {
          name: 'Credit (Paid)',
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
    const shopCounts = new Map<number, { name: string; count: number }>();

    this.customers.forEach(customer => {
      const shopId = customer.shopId || 0;
      const shopName = this.getShopName(customer.shopId);

      if (!shopCounts.has(shopId)) {
        shopCounts.set(shopId, { name: shopName, count: 0 });
      }
      const shop = shopCounts.get(shopId)!;
      shop.count++;
    });

    const chartData = Array.from(shopCounts.values()).map((shop, index) => ({
      name: shop.name,
      value: shop.count,
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
    const reversedEntryIds: number[] = this.ledgerEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    const effectiveEntries = this.ledgerEntries.filter(e =>
      !reversedEntryIds.includes(e.id!) && e.entryType !== LedgerEntryType.REVERSAL
    );

    const debitCount = effectiveEntries.filter(e => e.entryType === LedgerEntryType.BAKI).length;
    const creditCount = effectiveEntries.filter(e => e.entryType === LedgerEntryType.PAID).length;

    const data = [
      { name: 'Baki (Debit)', value: debitCount, itemStyle: { color: '#FF6B6B' } },
      { name: 'Paid (Credit)', value: creditCount, itemStyle: { color: '#51CF66' } }
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

  prepareTopCustomers(): void {
    // Top 10 customers with highest positive balance (Baki)
    const sorted = [...this.customers]
      .filter(c => c.currentBalance && c.currentBalance > 0)
      .sort((a, b) => (b.currentBalance || 0) - (a.currentBalance || 0));
    this.topCustomers = sorted.slice(0, 10);
    this.maxTopCustomerBalance = this.topCustomers.length > 0 ? this.topCustomers[0].currentBalance || 1 : 1;
  }

  getShopName(shopId: number | undefined): string {
    if (!shopId) return 'N/A';
    const shop = this.shops.find(s => s.id === shopId);
    return shop?.name || 'N/A';
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
