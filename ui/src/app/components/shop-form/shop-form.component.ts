import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ShopService } from '../../services/shop.service';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-shop-form',
    templateUrl: './shop-form.component.html',
    styleUrls: ['./shop-form.component.css'],
    standalone: false
})
export class ShopFormComponent implements OnInit {
  shop: Shop = {
    name: '',
    address: '',
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  isEditMode = false;
  shopId?: number;
  loading = false;

  constructor(
    private shopService: ShopService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.shopId = +params['id'];
        this.loadShop(this.shopId);
      }
    });
  }

  loadShop(id: number): void {
    this.loading = true;
    this.shopService.getShopById(id).subscribe({
      next: (shop) => {
        this.shop = shop;
        this.loading = false;
      },
      error: (error) => {
        this.router.navigate(['/shops']);
      }
    });
  }

  onSubmit(): void {
    this.loading = true;

    if (this.isEditMode && this.shopId) {
      this.shopService.updateShop(this.shopId, this.shop).subscribe({
        next: () => {
          this.loading = false;
          Swal.fire('Success!', 'Shop updated successfully!', 'success');
          this.router.navigate(['/shops']);
        },
        error: (error) => {
          this.loading = false;
        }
      });
    } else {
      this.shopService.createShop(this.shop).subscribe({
        next: () => {
          this.loading = false;
          Swal.fire('Success!', 'Shop created successfully!', 'success');
          this.router.navigate(['/shops']);
        },
        error: (error) => {
          this.loading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/shops']);
  }
}

