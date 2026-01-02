import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-customers',
  templateUrl: './customers.component.html',
  styleUrls: ['./customers.component.css'],
  standalone: false
})
export class CustomersComponent implements OnInit {
  customers: Customer[] = [];
  filteredCustomers: Customer[] = [];
  shops: Shop[] = [];
  loading = true;
  filterStatus: string = 'ALL';
  filterShop: string = '';
  searchTerm: string = '';

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
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadShops();
    this.loadCustomers();
  }

  loadShops(): void {
    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops.filter(s => s.isActive !== false);
      },
      error: (error) => {

      }
    });
  }

  loadCustomers(): void {
    this.loading = true;
    this.customerService.getAllCustomers().subscribe({
      next: (customers) => {
        this.customers = customers;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredCustomers = this.customers.filter(customer => {
      const matchesSearch = customer.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        customer.phone.includes(this.searchTerm);

      // Filter by status
      let matchesStatus = true;
      if (this.filterStatus === 'ACTIVE') {
        matchesStatus = customer.isActive !== false;
      } else if (this.filterStatus === 'INACTIVE') {
        matchesStatus = customer.isActive === false;
      }

      // Filter by shop
      let matchesShop = true;
      if (this.filterShop) {
        matchesShop = customer.shopId?.toString() === this.filterShop;
      }

      return matchesSearch && matchesStatus && matchesShop;
    });
    this.calculateSummaryStatistics();
  }

  calculateSummaryStatistics(): void {
    this.totalCustomers = this.customers.length;
    this.activeCustomers = this.customers.filter(c => c.isActive !== false).length;
    this.totalOpeningBalance = this.customers.reduce((sum, c) => sum + (c.openingBalance || 0), 0);
    this.totalCurrentBalance = this.customers.reduce((sum, c) => sum + (c.currentBalance || 0), 0);
    this.prepareCharts();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onSearchChange(): void {
    this.applyFilters();
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

          }
        });
      }
    });
  }

  getShopName(shopId: number | undefined): string {
    if (!shopId) return 'N/A';
    const shop = this.shops.find(s => s.id === shopId);
    return shop?.name || 'N/A';
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString();
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
    const activeCustomersWithBalance = this.filteredCustomers
      .filter(c => c.currentBalance !== 0)
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
          const sign = activeCustomersWithBalance.find((c: Customer) => c.name === params.name)?.currentBalance ?? 0 < 0 ? '-' : '+';
          return `${params.name}<br/>Balance: ${sign}₹${params.value.toFixed(2)}`;
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
    // Count customers by shop
    const shopCounts = new Map<number, { name: string; count: number }>();

    this.filteredCustomers.forEach(customer => {
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
}

