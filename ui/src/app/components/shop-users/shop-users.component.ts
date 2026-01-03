import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ShopService } from '../../services/shop.service';
import { Shop } from '../../models/shop.model';
import { ShopUser, ShopUserRole } from '../../models/shop-user.model';
import Swal from 'sweetalert2';
import { TimezoneService } from '../../services/timezone.service';

@Component({
  selector: 'app-shop-users',
  templateUrl: './shop-users.component.html',
  styleUrls: ['./shop-users.component.css'],
  standalone: false
})
export class ShopUsersComponent implements OnInit {
  shopId!: number;
  shop: Shop | null = null;
  shopUsers: ShopUser[] = [];
  loading = true;
  showAddUserModal = false;
  formSubmitting = false;

  // Form fields
  newUserPhone = '';
  selectedRole: 'OWNER' | 'STAFF' | 'VIEWER' = 'STAFF';
  phoneError = '';

  // Role definitions
  roles: ShopUserRole[] = [
    {
      id: 'OWNER',
      label: 'Owner',
      description: 'Full access to manage shop and users',
      icon: 'fas fa-crown'
    },
    {
      id: 'STAFF',
      label: 'Staff',
      description: 'Can manage shop operations and view reports',
      icon: 'fas fa-user-tie'
    },
    {
      id: 'VIEWER',
      label: 'Viewer',
      description: 'View only access to shop data',
      icon: 'fas fa-eye'
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private shopService: ShopService,
    private timezoneService: TimezoneService
  ) {}

  ngOnInit(): void {
    this.shopId = +this.route.snapshot.paramMap.get('shopId')!;
    this.loadShopAndUsers();
  }

  loadShopAndUsers(): void {
    this.loading = true;
    this.shopService.getShopById(this.shopId).subscribe({
      next: (shop) => {
        this.shop = shop;
        this.loadShopUsers();
      },
      error: (error) => {
        this.loading = false;
        this.router.navigate(['/shops']);
      }
    });
  }

  loadShopUsers(): void {
    this.shopService.getShopUsers(this.shopId).subscribe({
      next: (users) => {
        this.shopUsers = users;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
      }
    });
  }

  openAddUserModal(): void {
    this.resetForm();
    this.showAddUserModal = true;
  }

  closeAddUserModal(): void {
    this.showAddUserModal = false;
    this.resetForm();
  }

  resetForm(): void {
    this.newUserPhone = '';
    this.selectedRole = 'STAFF';
    this.phoneError = '';
    this.formSubmitting = false;
  }

  validatePhone(): boolean {
    const phoneRegex = /^\d{10}$/;
    if (!this.newUserPhone.trim()) {
      this.phoneError = 'Phone number is required';
      return false;
    }
    if (!phoneRegex.test(this.newUserPhone)) {
      this.phoneError = 'Phone number must be 10 digits';
      return false;
    }
    this.phoneError = '';
    return true;
  }

  addUser(): void {
    if (!this.validatePhone()) {
      return;
    }

    this.formSubmitting = true;
    const newUser: ShopUser = {
      shopId: this.shopId,
      userPhone: this.newUserPhone,
      role: this.selectedRole
    };

    this.shopService.addUserToShop(this.shopId, newUser).subscribe({
      next: (addedUser) => {
        this.shopUsers.push(addedUser);
        Swal.fire('Success!', 'User added to shop successfully', 'success');
        this.closeAddUserModal();
        this.loadShopUsers();
      },
      error: (error) => {
        this.formSubmitting = false;
      }
    });
  }

  updateUserRole(shopUser: ShopUser): void {
    const currentRole = shopUser.role;
    const availableRoles = this.roles.filter(r => r.id !== currentRole);

    Swal.fire({
      title: `Update Role for ${shopUser.userName}`,
      html: `
        <div class="flex flex-col gap-3">
          ${availableRoles.map(role => `
            <label class="flex items-center p-3 border-2 border-gray-200 rounded-lg cursor-pointer hover:border-indigo-500 transition-colors">
              <input type="radio" name="role" value="${role.id}" class="mr-3" />
              <div class="flex-1 text-left">
                <div class="font-semibold">${role.label}</div>
                <div class="text-sm text-gray-600">${role.description}</div>
              </div>
            </label>
          `).join('')}
        </div>
      `,
      icon: 'info',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Update Role',
      didOpen: () => {
        const radios = document.querySelectorAll('input[name="role"]');
        if (radios.length > 0) {
          (radios[0] as HTMLInputElement).checked = true;
        }
      }
    }).then((result) => {
      if (result.isConfirmed) {
        const radios = document.querySelectorAll('input[name="role"]');
        const selectedRadio = Array.from(radios).find((r: any) => r.checked) as HTMLInputElement;
        if (selectedRadio) {
          const newRole = selectedRadio.value as 'OWNER' | 'STAFF' | 'VIEWER';
          if (shopUser.id) {
            this.shopService.updateUserRole(this.shopId, shopUser.id, newRole).subscribe({
              next: () => {
                shopUser.role = newRole;
                Swal.fire('Success!', 'User role updated successfully', 'success');
              },
              error: (error) => {
                const errorMessage = error.error?.message || 'Failed to update user role';
              }
            });
          }
        }
      }
    });
  }

  removeUser(shopUser: ShopUser): void {
    Swal.fire({
      title: 'Remove User?',
      text: `Are you sure you want to remove ${shopUser.userName} from this shop?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Yes, remove user'
    }).then((result) => {
      if (result.isConfirmed && shopUser.id) {
        this.shopService.removeUserFromShop(this.shopId, shopUser.id).subscribe({
          next: () => {
            this.shopUsers = this.shopUsers.filter(u => u.id !== shopUser.id);
            Swal.fire('Success!', 'User removed from shop', 'success');
          },
          error: (error) => {
            const errorMessage = error.error?.message || 'Failed to remove user';
          }
        });
      }
    });
  }

  getRoleLabel(role: string): string {
    return this.roles.find(r => r.id === role)?.label || role;
  }

  getRoleIcon(role: string): string {
    return this.roles.find(r => r.id === role)?.icon || 'fas fa-user';
  }

  getRoleColor(role: string): string {
    switch (role) {
      case 'OWNER':
        return 'bg-purple-100 text-purple-800';
      case 'STAFF':
        return 'bg-blue-100 text-blue-800';
      case 'VIEWER':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatDate(date: Date): string {
    return this.timezoneService.formatDateInIndiaTimezone(date, {});
  }

  goBack(): void {
    this.router.navigate(['/shops']);
  }
}

