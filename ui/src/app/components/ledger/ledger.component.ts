import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { LedgerService } from '../../services/ledger.service';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { CustomerLedger, LedgerEntryType } from '../../models/ledger.model';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';
import {TimezoneService} from "../../services/timezone.service";

@Component({
  selector: 'app-ledger',
  templateUrl: './ledger.component.html',
  styleUrls: ['./ledger.component.css'],
  standalone: false
})
export class LedgerComponent implements OnInit {
  ledgerEntries: CustomerLedger[] = [];
  filteredEntries: CustomerLedger[] = [];
  customers: Customer[] = [];
  shops: Shop[] = [];
  loading = true;

  // Filters
  filterShop: string = '';
  filterCustomer: string = '';
  filterType: string = '';
  startDate: string = '';
  endDate: string = '';

  // Summary statistics
  totalDebit: number = 0;
  totalCredit: number = 0;
  netBalance: number = 0;
  totalEntries: number = 0;

  // Chart options
  debitCreditTrendChartOptions: any = {};
  entryTypeDistributionChartOptions: any = {};
  dailyTransactionChartOptions: any = {};
  hasDebitCreditData = false;
  hasEntryTypeData = false;
  hasDailyData = false;

  constructor(
    private ledgerService: LedgerService,
    private customerService: CustomerService,
    private shopService: ShopService,
    private router: Router,
    private route: ActivatedRoute,
    private timezoneService: TimezoneService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['customerId']) {
        this.filterCustomer = params['customerId'];
      }
    });
    this.loadShops();
    this.loadCustomers();
    this.loadLedgerEntries();
    this.setDefaultDateRange();
  }

  setDefaultDateRange(): void {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
    const lastDay = today;

    this.startDate = firstDay.toISOString().split('T')[0];
    this.endDate = lastDay.toISOString().split('T')[0];
  }

  loadShops(): void {
    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops.filter(s => s.isActive !== false);
      },
      error: (error) => {
        console.error('Error loading shops:', error);
      }
    });
  }

  loadCustomers(): void {
    this.customerService.getAllCustomers().subscribe({
      next: (customers) => {
        this.customers = customers;
      },
      error: (error) => {
      }
    });
  }

  loadLedgerEntries(): void {
    this.loading = true;
    this.ledgerService.getAllLedgerEntries().subscribe({
      next: (entries) => {
        this.ledgerEntries = entries.sort((a, b) => {
          return new Date(b.entryDate).getTime() - new Date(a.entryDate).getTime();
        });
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredEntries = this.ledgerEntries.filter(entry => {
      // Filter by shop
      let matchesShop = true;
      if (this.filterShop) {
        matchesShop = entry.shopId?.toString() === this.filterShop;
      }

      // Filter by customer
      let matchesCustomer = true;
      if (this.filterCustomer) {
        matchesCustomer = entry.customerId?.toString() === this.filterCustomer;
      }

      // Filter by entry type
      let matchesType = true;
      if (this.filterType) {
        matchesType = entry.entryType === this.filterType;
      }

      // Filter by date range
      let matchesDate = true;
      if (this.startDate || this.endDate) {
        const entryDate = new Date(entry.entryDate);
        if (this.startDate) {
          matchesDate = matchesDate && entryDate >= new Date(this.startDate);
        }
        if (this.endDate) {
          matchesDate = matchesDate && entryDate <= new Date(this.endDate);
        }
      }

      return matchesShop && matchesCustomer && matchesType && matchesDate;
    });
    this.calculateSummaryStatistics();
  }

  calculateSummaryStatistics(): void {
    // Get all entry IDs that have been reversed
    const reversedEntryIds: number[] = this.filteredEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    // Filter out reversed entries and reversals themselves
    const effectiveEntries = this.filteredEntries.filter(e =>
      !reversedEntryIds.includes(e.id!) && e.entryType !== LedgerEntryType.REVERSAL
    );

    // Calculate based on entry type, excluding reversals and reversed entries
    this.totalDebit = effectiveEntries
      .filter(e => e.entryType === LedgerEntryType.BAKI)
      .reduce((sum, e) => sum + (e.amount || 0), 0);

    this.totalCredit = effectiveEntries
      .filter(e => e.entryType === LedgerEntryType.PAID)
      .reduce((sum, e) => sum + (e.amount || 0), 0);

    this.netBalance = this.totalDebit - this.totalCredit;
    this.totalEntries = effectiveEntries.length;

    this.prepareCharts();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  viewLedgerEntry(entry: CustomerLedger): void {
    Swal.fire({
      title: 'Ledger Entry Details',
      html: `
        <div class="text-left space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">ID</p>
              <p class="text-sm font-semibold text-gray-700">${entry.id || 'N/A'}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Entry Date</p>
              <p class="text-sm font-semibold text-gray-700">${this.formatDate(entry.entryDate)}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Customer</p>
              <p class="text-sm font-semibold text-gray-700">${entry.customer?.name || 'N/A'}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Shop</p>
              <p class="text-sm font-semibold text-gray-700">${this.getShopName(entry.shopId)}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Entry Type</p>
              <p class="text-sm font-semibold text-gray-700">${entry.entryType}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Amount</p>
              <p class="text-sm font-semibold text-gray-700">${this.formatCurrency(entry.amount)}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Balance After</p>
              <p class="text-sm font-semibold text-gray-700">${this.formatCurrency(entry.balanceAfter)}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Created By</p>
              <p class="text-sm font-semibold text-gray-700">${entry.createdByUser?.name || 'N/A'}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Created At</p>
              <p class="text-sm font-semibold text-gray-700">${this.formatDateTime(entry.createdAt) || 'N/A'}</p>
            </div>
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 uppercase">Reference Entry ID</p>
              <p class="text-sm font-semibold text-gray-700">${entry.referenceEntryId || '-'}</p>
            </div>
          </div>
          ${entry.notes ? `
            <div class="space-y-2 border-t pt-4">
              <p class="text-xs font-medium text-gray-500 uppercase">Notes</p>
              <p class="text-sm text-gray-700 bg-gray-50 p-3 rounded">${entry.notes}</p>
            </div>
          ` : ''}
        </div>
      `,
      icon: 'info',
      confirmButtonText: 'Close',
      width: 600
    });
  }

  hasBeenReversed(entryId: number | undefined): boolean {
    if (!entryId) return false;
    return this.ledgerEntries.some(e =>
      e.entryType === LedgerEntryType.REVERSAL && e.referenceEntryId === entryId
    );
  }

  addNewEntry(): void {
    this.router.navigate(['/ledger/new']);
  }

  viewCustomerLedger(customerId: number): void {
    this.router.navigate(['/ledger/customer', customerId]);
  }

  reverseLedgerEntry(entry: CustomerLedger): void {
    if (entry.entryType === LedgerEntryType.REVERSAL) {
      Swal.fire('Warning!', 'This entry is already a reversal and cannot be reversed again', 'warning');
      return;
    }

    if (this.hasBeenReversed(entry.id)) {
      Swal.fire('Warning!', 'This entry has already been reversed and cannot be reversed again', 'warning');
      return;
    }

    Swal.fire({
      title: 'Reverse Entry?',
      html: `
        <div class="text-left space-y-3">
          <p>Are you sure you want to reverse this ledger entry?</p>
          <div class="bg-gray-50 p-3 rounded text-sm space-y-1">
            <p><strong>Customer:</strong> ${entry.customer?.name || 'N/A'}</p>
            <p><strong>Type:</strong> ${entry.entryType}</p>
            <p><strong>Amount:</strong> ${entry.amount}</p>
            <p><strong>Date:</strong> ${this.formatDate(entry.entryDate)}</p>
          </div>
          <textarea id="reversalNotes" placeholder="Add notes for reversal (optional)" 
            class="w-full px-3 py-2 border border-gray-300 rounded text-sm h-20"></textarea>
        </div>
      `,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Yes, reverse it!'
    }).then((result) => {
      if (result.isConfirmed) {
        const notes = (document.getElementById('reversalNotes') as HTMLTextAreaElement)?.value;
        this.ledgerService.reverseLedgerEntry(entry.id!, notes).subscribe({
          next: () => {
            Swal.fire('Success!', 'Ledger entry reversed successfully!', 'success');
            this.loadLedgerEntries();
          },
          error: (error) => {
            console.error('Error reversing entry:', error);
          }
        });
      }
    });
  }

  getCustomerName(customerId: number | undefined): string {
    if (!customerId) return 'N/A';
    const customer = this.customers.find(c => c.id === customerId);
    return customer?.name || 'N/A';
  }

  getShopName(shopId: number | undefined): string {
    if (!shopId) return 'N/A';
    const shop = this.shops.find(s => s.id === shopId);
    return shop?.name || 'N/A';
  }

  getEntryTypeIcon(type: LedgerEntryType): string {
    switch (type) {
      case LedgerEntryType.BAKI:
        return 'fa-arrow-down text-red-600';
      case LedgerEntryType.PAID:
        return 'fa-arrow-up text-emerald-600';
      case LedgerEntryType.REVERSAL:
        return 'fa-undo text-orange-600';
      default:
        return 'fa-exchange text-gray-600';
    }
  }

  getEntryTypeColor(type: LedgerEntryType): string {
    switch (type) {
      case LedgerEntryType.BAKI:
        return 'bg-red-100 text-red-800';
      case LedgerEntryType.PAID:
        return 'bg-emerald-100 text-emerald-800';
      case LedgerEntryType.REVERSAL:
        return 'bg-orange-100 text-orange-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatDate(date: Date): string {
    return this.timezoneService.formatDateOnlyInIndiaTimezone(date, {});
  }

  formatDateTime(date: Date): string {
    return this.timezoneService.formatDateInIndiaTimezone(date, {});
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

  // Chart preparation methods
  prepareCharts(): void {
    this.prepareDebitCreditTrendChart();
    this.prepareEntryTypeDistributionChart();
    this.prepareDailyTransactionChart();
  }

  prepareDebitCreditTrendChart(): void {
    // Get daily debit and credit totals
    const dailyData = new Map<string, { debit: number; credit: number }>();

    const reversedEntryIds: number[] = this.filteredEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    const effectiveEntries = this.filteredEntries.filter(e =>
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

    const dates = Array.from(dailyData.keys()).sort();
    const debits = dates.map(date => dailyData.get(date)!.debit);
    const credits = dates.map(date => dailyData.get(date)!.credit);

    this.hasDebitCreditData = dates.length > 0;

    const isMobile = window.innerWidth < 640;

    this.debitCreditTrendChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any) => {
          if (!Array.isArray(params)) params = [params];
          return params.map((p: any) => `${p.seriesName}: ₹${p.value.toFixed(2)}`).join('<br/>');
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

  prepareEntryTypeDistributionChart(): void {
    const reversedEntryIds: number[] = this.filteredEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    const effectiveEntries = this.filteredEntries.filter(e =>
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

    this.entryTypeDistributionChartOptions = {
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

  prepareDailyTransactionChart(): void {
    // Get daily transaction counts
    const dailyCounts = new Map<string, number>();

    const reversedEntryIds: number[] = this.filteredEntries
      .filter(e => e.entryType === LedgerEntryType.REVERSAL)
      .map(e => e.referenceEntryId)
      .filter((id): id is number => id !== undefined);

    const effectiveEntries = this.filteredEntries.filter(e =>
      !reversedEntryIds.includes(e.id!) && e.entryType !== LedgerEntryType.REVERSAL
    );

    effectiveEntries.forEach(entry => {
      const date = new Date(entry.entryDate).toLocaleDateString();
      dailyCounts.set(date, (dailyCounts.get(date) || 0) + 1);
    });

    const dates = Array.from(dailyCounts.keys()).sort();
    const counts = dates.map(date => dailyCounts.get(date) || 0);

    this.hasDailyData = dates.length > 0;

    const isMobile = window.innerWidth < 640;

    this.dailyTransactionChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          if (!Array.isArray(params)) params = [params];
          return params.map((p: any) => `${p.name}: ${p.value} transactions`).join('<br/>');
        }
      },
      grid: {
        left: isMobile ? 40 : 50,
        right: isMobile ? 10 : 20,
        top: 20,
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
          data: counts.map(count => ({ value: count, itemStyle: { color: '#4F46E5' } })),
          type: 'bar',
          itemStyle: {
            borderRadius: [8, 8, 0, 0]
          },
          emphasis: {
            focus: 'series'
          }
        }
      ]
    };
  }
}
