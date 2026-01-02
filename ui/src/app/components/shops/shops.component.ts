import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ShopService } from '../../services/shop.service';
import { Shop } from '../../models/shop.model';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-shops',
    templateUrl: './shops.component.html',
    styleUrls: ['./shops.component.css'],
    standalone: false
})
export class ShopsComponent implements OnInit {
    shops: Shop[] = [];
    filteredShops: Shop[] = [];
    loading = true;
    filterStatus: string = 'ALL';
    searchTerm: string = '';

    // Summary statistics
    totalShops: number = 0;
    activeShops: number = 0;

    constructor(
        private shopService: ShopService,
        private router: Router
    ) {}

    ngOnInit(): void {
        this.loadShops();
    }

    loadShops(): void {
        this.loading = true;
        this.shopService.getAllShops().subscribe({
            next: (shops) => {
                this.shops = shops;
                this.applyFilters();
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading shops:', error);
                Swal.fire('Error!', 'Failed to load shops', 'error');
                this.loading = false;
            }
        });
    }

    applyFilters(): void {
        this.filteredShops = this.shops.filter(shop => {
            const matchesSearch = shop.name.toLowerCase().includes(this.searchTerm.toLowerCase());

            // Filter by status
            let matchesStatus = true;
            if (this.filterStatus === 'ACTIVE') {
                matchesStatus = shop.isActive !== false;
            } else if (this.filterStatus === 'INACTIVE') {
                matchesStatus = shop.isActive === false;
            }
            // If 'ALL', matchesStatus remains true

            return matchesSearch && matchesStatus;
        });
        this.calculateSummaryStatistics();
    }

    calculateSummaryStatistics(): void {
        this.totalShops = this.shops.length;
        this.activeShops = this.shops.filter(s => s.isActive !== false).length;
    }

    onFilterChange(): void {
        this.applyFilters();
    }

    onSearchChange(): void {
        this.applyFilters();
    }

    editShop(shop: Shop): void {
        this.router.navigate(['/shops/edit', shop.id]);
    }

    toggleShopStatus(shop: Shop): void {
        const newStatus = !shop.isActive;
        const action = newStatus ? 'activate' : 'deactivate';
        const actionText = newStatus ? 'activate' : 'deactivate';

        Swal.fire({
            title: 'Are you sure?',
            text: `Do you want to ${actionText} the shop "${shop.name}"?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: `Yes, ${actionText} it!`
        }).then((result) => {
            if (result.isConfirmed) {
                const updatedShop = { ...shop, isActive: newStatus };
                this.shopService.updateShop(shop.id!, updatedShop).subscribe({
                    next: () => {
                        const message = newStatus ? 'Shop activated successfully!' : 'Shop deactivated successfully!';
                        Swal.fire('Success!', message, 'success');
                        this.loadShops();
                    },
                    error: (error) => {
                        console.error('Error updating shop status:', error);
                        Swal.fire('Error!', 'Failed to update shop status', 'error');
                    }
                });
            }
        });
    }

    addNewShop(): void {
        this.router.navigate(['/shops/new']);
    }

    formatDate(date: string | undefined): string {
        if (!date) return 'N/A';
        return new Date(date).toLocaleDateString();
    }
}


