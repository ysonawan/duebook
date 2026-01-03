import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { LedgerService } from '../../services/ledger.service';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { CustomerLedger, LedgerEntryType } from '../../models/ledger.model';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';
import { TimezoneService } from '../../services/timezone.service';

@Component({
  selector: 'app-ledger',
  templateUrl: './ledger.component.html',
  styleUrls: ['./ledger.component.css'],
  standalone: false
})
export class LedgerComponent implements OnInit {
  ledgerEntries: CustomerLedger[] = [];
  customers: Customer[] = [];
  shops: Shop[] = [];
  loading = true;
  selectedShopId: number = 0;
  showFilters: boolean = false;

  // Filters
  filterCustomer: number = 0;
  filterType: string = '';
  startDate: string = '';
  endDate: string = '';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 20;
  totalElements: number = 0;
  totalPages: number = 0;

  // Summary statistics
  totalDebit: number = 0;
  totalCredit: number = 0;
  netBalance: number = 0;
  totalEntries: number = 0;

  // Detail Modal
  selectedEntry: CustomerLedger | null = null;
  showDetailModal = false;

  constructor(
    private ledgerService: LedgerService,
    private customerService: CustomerService,
    private shopService: ShopService,
    private router: Router,
    private route: ActivatedRoute,
    private timezoneService: TimezoneService
  ) {}

  ngOnInit(): void {
    // Set selected shop to "All Shops" by default (null)
    this.selectedShopId = 0;
    this.route.queryParams.subscribe(params => {
      if (params['customerId']) {
        this.filterCustomer = params['customerId'];
      }
      if (params['shopId']) {
        this.selectedShopId = params['shopId'];
      }
    });
    this.loadShops();
    this.loadCustomers();
  }

  loadShops(): void {
    this.shopService.getAllShops().subscribe({
      next: (shops) => {
        this.shops = shops.filter(s => s.isActive !== false);
        this.loadLedgerEntries();
      },
      error: (error) => {
        console.error('Error loading shops:', error);
        this.loading = false;
      }
    });
  }

  loadCustomers(): void {
    this.customerService.getAllCustomers().subscribe({
      next: (customers) => {
        this.customers = customers;
      },
      error: (error) => {
        console.error('Error loading customers:', error);
      }
    });
  }

  onShopChange(): void {
    this.currentPage = 0;
    this.filterCustomer = 0;
    this.filterType = '';
    this.startDate = '';
    this.endDate = '';
    this.loadLedgerEntries();
  }

  loadLedgerEntries(): void {
    this.loading = true;

    // Use 0 to indicate "all shops" - the backend will handle it
    const shopId = this.selectedShopId || 0;

    this.ledgerService.getLedgerEntriesPaginated(
      shopId,
      this.currentPage,
      this.pageSize,
      this.filterCustomer || undefined,
      this.filterType,
      this.startDate,
      this.endDate
    ).subscribe({
      next: (response: any) => {
        this.ledgerEntries = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loadLedgerSummary();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading ledger entries:', error);
        this.loading = false;
      }
    });
  }

  /**
   * Load ledger summary with applied filters (all records, not paginated)
   */
  loadLedgerSummary(): void {
    const shopId = this.selectedShopId || 0;

    this.ledgerService.getLedgerSummary(
      shopId,
      this.filterCustomer || undefined,
      this.filterType,
      this.startDate,
      this.endDate
    ).subscribe({
      next: (summary: any) => {
        this.totalDebit = summary.totalDebit || 0;
        this.totalCredit = summary.totalCredit || 0;
        this.netBalance = summary.netBalance || 0;
        this.totalEntries = summary.totalEntries || 0;
      },
      error: (error) => {
        console.error('Error loading ledger summary:', error);
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadLedgerEntries();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadLedgerEntries();
  }

  viewLedgerEntry(entry: CustomerLedger): void {
    this.selectedEntry = entry;
    this.showDetailModal = true;
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedEntry = null;
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
            <p><strong>Customer:</strong> ${this.getCustomerName(entry.customerId)}</p>
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

