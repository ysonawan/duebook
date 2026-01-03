import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';
import {TimezoneService} from "../../services/timezone.service";

@Component({
  selector: 'app-customers',
  templateUrl: './customers.component.html',
  styleUrls: ['./customers.component.css'],
  standalone: false
})
export class CustomersComponent implements OnInit {
  customers: Customer[] = [];
  shops: Shop[] = [];
  loading = true;
  selectedShopId: number = 0;

  // Filters
  filterStatus: string = '';
  searchTerm: string = '';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 20;
  totalElements: number = 0;
  totalPages: number = 0;

  // Summary statistics
  totalCustomers: number = 0;
  activeCustomers: number = 0;
  totalOpeningBalance: number = 0;
  totalCurrentBalance: number = 0;

  // Chart options
  balanceDistributionChartOptions: any = {};
  customersByShopChartOptions: any = {};
  hasBalanceChartData = false;
  hasShopChartData = false;

  constructor(
    private customerService: CustomerService,
    private shopService: ShopService,
    private router: Router,
    private timezoneService: TimezoneService
  ) {}

  ngOnInit(): void {
    this.loadShops();
  }

  loadShops(): void {
    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops.filter(s => s.isActive !== false);

        // Set selected shop to "All Shops" by default (null)
        this.selectedShopId = 0;
        this.loadCustomers();
      },
      error: (error) => {
        console.error('Error loading shops:', error);
        this.loading = false;
      }
    });
  }

  onShopChange(): void {
    this.currentPage = 0;
    this.filterStatus = '';
    this.searchTerm = '';
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loading = true;

    // Use 0 to indicate "all shops" - the backend will handle it
    const shopId = this.selectedShopId || 0;

    this.customerService.getCustomersPaginated(
      shopId,
      this.currentPage,
      this.pageSize,
      this.filterStatus,
      this.searchTerm
    ).subscribe({
      next: (response: any) => {
        this.customers = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.calculateSummaryStatistics();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading customers:', error);
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadCustomers();
  }

  onSearchChange(): void {
    this.currentPage = 0;
    this.loadCustomers();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadCustomers();
  }

  calculateSummaryStatistics(): void {
    this.totalCustomers = this.totalElements;
    this.activeCustomers = this.customers.filter(c => c.isActive !== false).length;
    this.totalOpeningBalance = this.customers.reduce((sum, c) => sum + (c.openingBalance || 0), 0);
    this.totalCurrentBalance = this.customers.reduce((sum, c) => sum + (c.currentBalance || 0), 0);
    this.prepareCharts();
  }

  addNewCustomer(): void {
    this.router.navigate(['/customers/new']);
  }

  editCustomer(customer: Customer): void {
    this.router.navigate(['/customers/edit', customer.id]);
  }

  toggleCustomerStatus(customer: Customer): void {
    const newStatus = !customer.isActive;
    const actionText = newStatus ? 'activate' : 'deactivate';

    Swal.fire({
      title: 'Are you sure?',
      text: `Do you want to ${actionText} the customer "${customer.name}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: `Yes, ${actionText} it!`
    }).then((result) => {
      if (result.isConfirmed) {
        const updatedCustomer = { ...customer, isActive: newStatus };
        this.customerService.updateCustomer(customer.id!, updatedCustomer).subscribe({
          next: () => {
            const message = newStatus ? 'Customer activated successfully!' : 'Customer deactivated successfully!';
            Swal.fire('Success!', message, 'success');
            this.loadCustomers();
          },
          error: (error) => {
            console.error('Error updating customer:', error);
          }
        });
      }
    });
  }

  viewLedger(customer: Customer) {
    this.router.navigate(['/ledger'], { queryParams: { customerId: customer.id } });
  }

  getShopName(shopId: number | undefined): string {
    if (!shopId) return 'N/A';
    const shop = this.shops.find(s => s.id === shopId);
    return shop?.name || 'N/A';
  }

  formatDate(date: Date): string {
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
    this.prepareBalanceDistributionChart();
    this.prepareCustomersByShopChart();
  }

  prepareBalanceDistributionChart(): void {
    // Filter customers with balance > 0
    const activeCustomersWithBalance = this.customers
      .filter(c => c.currentBalance !== 0 && c.currentBalance > 0)
      .sort((a, b) => Math.abs(b.currentBalance) - Math.abs(a.currentBalance))
      .slice(0, 10); // Top 10 customers

    const data = activeCustomersWithBalance.map((customer, index) => ({
      name: customer.name,
      value: Math.abs(customer.currentBalance),
      itemStyle: { color: this.getColorForIndex(index) }
    }));

    this.hasBalanceChartData = data.length > 0;

    const isMobile = window.innerWidth < 640;

    this.balanceDistributionChartOptions = {
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          return `${params.name}<br/>Balance: ₹${params.value.toFixed(2)}`;
        },
        confine: true
      },
      legend: {
        orient: isMobile ? 'horizontal' : 'vertical',
        bottom: isMobile ? 0 : undefined,
        right: isMobile ? undefined : 10,
        top: isMobile ? undefined : 'center',
        left: isMobile ? 'center' : undefined,
        textStyle: { fontSize: isMobile ? 10 : 11 },
        itemWidth: 12,
        itemHeight: 12,
        itemGap: isMobile ? 8 : 10,
        padding: isMobile ? [0, 5] : [5, 5]
      },
      series: [
        {
          name: 'Customer Balance',
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

  prepareCustomersByShopChart(): void {
    // For single shop, create simple distribution
    const activeCustomers = this.customers.filter(c => c.isActive !== false).length;
    const inactiveCustomers = this.customers.filter(c => c.isActive === false).length;

    const chartData = [
      { name: 'Active', value: activeCustomers, itemStyle: { color: '#10b981' } },
      { name: 'Inactive', value: inactiveCustomers, itemStyle: { color: '#d1d5db' } }
    ];

    this.hasShopChartData = chartData.some(d => d.value > 0);

    const isMobile = window.innerWidth < 640;

    this.customersByShopChartOptions = {
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
          interval: 0,
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

  getColorForIndex(index: number): string {
    const colors = [
      '#FF8C00', '#0099CC', '#99CC00', '#9900CC', '#00CC99',
      '#FF00FF', '#808080', '#00FF00', '#FF0000', '#00FFFF',
      '#FF6600', '#9900FF', '#0000FF', '#00FF99', '#CC00FF',
      '#CC9900', '#FF0099', '#99FF00', '#0099FF', '#CC0099'
    ];
    return colors[index % colors.length];
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const startPage = Math.max(0, this.currentPage - 2);
    const endPage = Math.min(this.totalPages - 1, this.currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  Math = Math;
}
