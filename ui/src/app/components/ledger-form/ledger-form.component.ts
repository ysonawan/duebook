import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LedgerService } from '../../services/ledger.service';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { CustomerLedger, LedgerEntryType } from '../../models/ledger.model';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-ledger-form',
  templateUrl: './ledger-form.component.html',
  styleUrls: ['./ledger-form.component.css'],
  standalone: false
})
export class LedgerFormComponent implements OnInit {
  ledgerEntry: CustomerLedger = {
    customerId: 0,
    shopId: 0,
    entryType: LedgerEntryType.BAKI,
    amount: 0,
    balanceAfter: 0,
    entryDate: new Date(),
    createdAt: new Date(),
    notes: ''
  };

  customers: Customer[] = [];
  shops: Shop[] = [];
  filteredCustomers: Customer[] = [];
  loading = false;
  selectedCustomer: Customer | null = null;
  selectedShop: Shop | null = null;
  currentBalance: number = 0;

  LedgerEntryType = LedgerEntryType;
  entryTypes = Object.values(LedgerEntryType).filter(type => type !== LedgerEntryType.REVERSAL);

  constructor(
    private ledgerService: LedgerService,
    private customerService: CustomerService,
    private shopService: ShopService,
    private router: Router,
    private route: ActivatedRoute
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
    this.customerService.getAllCustomers().subscribe({
      next: (customers) => {
        this.customers = customers.filter(c => c.isActive !== false);
      },
      error: (error) => {
      }
    });
  }

  onShopChange(): void {
    if (this.ledgerEntry.shopId && Number(this.ledgerEntry.shopId) !== 0) {
      this.selectedShop = this.shops.find(s => s.id === Number(this.ledgerEntry.shopId)) || null;
      // Fetch customers for the selected shop
      this.customerService.getCustomersByShop(this.ledgerEntry.shopId).subscribe({
        next: (customers) => {
          this.filteredCustomers = customers.filter(c => c.isActive !== false);
          console.log('Filtered customers for shop:', this.filteredCustomers);
        },
        error: (error) => {
          this.filteredCustomers = [];
        }
      });
      // Reset customer selection when shop changes
      this.ledgerEntry.customerId = 0;
    } else {
      this.filteredCustomers = [];
    }

    this.selectedCustomer = null;
    this.currentBalance = 0;
    this.ledgerEntry.balanceAfter = 0;
    this.ledgerEntry.amount = 0;
    this.ledgerEntry.entryType = LedgerEntryType.BAKI;
  }

  onCustomerChange(): void {
    if (this.ledgerEntry.customerId && Number(this.ledgerEntry.customerId) !== 0) {
      const customer = this.filteredCustomers.find(c => c.id === Number(this.ledgerEntry.customerId));
      if (customer) {
        this.selectedCustomer = customer;
        this.currentBalance = customer.currentBalance || 0;
        this.calculateBalanceAfter();
      }
    }
  }

  calculateBalanceAfter(): void {
    if (!this.selectedCustomer) return;

    const currentBalance = this.selectedCustomer.currentBalance || 0;
    const amount = this.ledgerEntry.amount || 0;

    if (this.ledgerEntry.entryType === LedgerEntryType.BAKI) {
      // BAKI increases balance
      this.ledgerEntry.balanceAfter = currentBalance + amount;
    } else if (this.ledgerEntry.entryType === LedgerEntryType.PAID) {
      // PAID decreases balance
      this.ledgerEntry.balanceAfter = currentBalance - amount;
    }
  }

  onAmountChange(): void {
    this.calculateBalanceAfter();
  }

  onTypeChange(): void {
    this.calculateBalanceAfter();
  }

  onSubmit(): void {
    this.loading = true;

    this.ledgerService.createLedgerEntry(this.ledgerEntry).subscribe({
      next: () => {
        this.loading = false;
        Swal.fire('Success!', 'Ledger entry created successfully!', 'success');
        this.router.navigate(['/ledger']);
      },
      error: (error) => {
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/ledger']);
  }

  getEntryTypeLabel(type: string): string {
    if (type === LedgerEntryType.BAKI) return 'Debit-Increase Balance (बाकी)';
    if (type === LedgerEntryType.PAID) return 'Credit-Decrease Balance (जमा)';
    if (type === LedgerEntryType.REVERSAL) return 'Reversal-Undo Entry (उलट)';
    return type;
  }
}

