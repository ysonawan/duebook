import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { ShopService } from '../../services/shop.service';
import { Customer } from '../../models/customer.model';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-customer-form',
  templateUrl: './customer-form.component.html',
  styleUrls: ['./customer-form.component.css'],
  standalone: false
})
export class CustomerFormComponent implements OnInit {
  customer: Customer = {
    name: '',
    entityName: '',
    phone: '',
    openingBalance: 0,
    currentBalance: 0,
    shopId: undefined,
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  shops: Shop[] = [];
  isEditMode = false;
  customerId?: number;
  loading = false;

  constructor(
    private customerService: CustomerService,
    private shopService: ShopService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadShops();
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.customerId = +params['id'];
        this.loadCustomer(this.customerId);
      }
    });
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

  loadCustomer(id: number): void {
    this.loading = true;
    this.customerService.getCustomerById(id).subscribe({
      next: (customer) => {
        this.customer = customer;
        this.loading = false;
      },
      error: (error) => {
        this.router.navigate(['/customers']);
      }
    });
  }

  onSubmit(): void {

    this.loading = true;

    if (this.isEditMode && this.customerId) {
      this.customerService.updateCustomer(this.customerId, this.customer).subscribe({
        next: () => {
          this.loading = false;
          Swal.fire('Success!', 'Customer updated successfully!', 'success');
          this.router.navigate(['/customers']);
        },
        error: (error) => {
          this.loading = false;
        }
      });
    } else {
      this.customerService.createCustomer(this.customer).subscribe({
        next: () => {
          this.loading = false;
          Swal.fire('Success!', 'Customer created successfully!', 'success');
          this.router.navigate(['/customers']);
        },
        error: (error) => {
          this.loading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/customers']);
  }
}

