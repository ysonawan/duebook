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
  showFilters: boolean = false;

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

  // Detail Modal
  selectedCustomer: Customer | null = null;
  showDetailModal = false;

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
        this.loadSummary();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading customers:', error);
        this.loading = false;
      }
    });
  }

  /**
   * Load summary statistics with applied filters (all records, not paginated)
   */
  loadSummary(): void {
    const shopId = this.selectedShopId || 0;

    this.customerService.getCustomerSummary(
      shopId,
      this.filterStatus,
      this.searchTerm
    ).subscribe({
      next: (summary: any) => {
        this.totalCustomers = summary.totalCustomers || 0;
        this.activeCustomers = summary.activeCustomers || 0;
        this.totalOpeningBalance = summary.totalOpeningBalance || 0;
        this.totalCurrentBalance = summary.totalCurrentBalance || 0;
      },
      error: (error) => {
        console.error('Error loading customer summary:', error);
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

  addNewCustomer(): void {
    this.router.navigate(['/customers/new']);
  }

  viewDetails(customer: Customer): void {
    this.selectedCustomer = customer;
    this.showDetailModal = true;
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCustomer = null;
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
    this.router.navigate(['/ledger'], { queryParams: { customerId: customer.id, shopId: customer.shopId } });
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
