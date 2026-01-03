import { Component, OnInit } from '@angular/core';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditLog } from '../../models/audit-log.model';
import { Shop } from '../../models/shop.model';
import { ShopService } from '../../services/shop.service';
import { TimezoneService } from '../../services/timezone.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.css'],
  standalone: false
})
export class AuditLogsComponent implements OnInit {
  auditLogs: AuditLog[] = [];
  loading = true;
  shops: Shop[] = [];
  selectedShopId: number = 0;

  // Filters
  filterAction: string = '';
  filterEntityType: string = '';
  startDate: string = '';
  endDate: string = '';

  // Filter options (fetched from database)
  availableActions: string[] = [];
  availableEntityTypes: string[] = [];

  // Pagination
  currentPage: number = 0;
  pageSize: number = 20;
  totalElements: number = 0;
  totalPages: number = 0;

  // Modal
  selectedLog: AuditLog | null = null;
  showDetailModal = false;

  constructor(
    private auditLogService: AuditLogService,
    private shopService: ShopService,
    private timezoneService: TimezoneService
  ) {}

  ngOnInit(): void {
    this.loadFilterOptions();
    this.loadShops();
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

        // Set selected shop to "All Shops" by default (null)
        this.selectedShopId = 0;
        this.loadAuditLogs();
      },
      error: (error) => {
        console.error('Error loading shops:', error);
        this.loading = false;
      }
    });
  }

  getShopName(shopId: number | undefined): string {
    if (!shopId) return 'N/A';
    const shop = this.shops.find(s => s.id === shopId);
    return shop?.name || 'N/A';
  }

  loadFilterOptions(): void {
    // Load available actions
    this.auditLogService.getDistinctActions().subscribe({
      next: (actions) => {
        this.availableActions = actions;
      },
      error: (error) => {
        console.error('Error loading actions:', error);
      }
    });

    // Load available entity types
    this.auditLogService.getDistinctEntityTypes().subscribe({
      next: (types) => {
        this.availableEntityTypes = types;
      },
      error: (error) => {
        console.error('Error loading entity types:', error);
      }
    });
  }

  onShopChange(): void {
    this.currentPage = 0;
    this.filterAction = '';
    this.filterEntityType = '';
    this.startDate = '';
    this.endDate = '';
    this.setDefaultDateRange();
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.loading = true;

    // Use 0 or null to indicate "all shops" - the backend will handle it
    const shopId = this.selectedShopId || 0;

    this.auditLogService.getAuditLogsPaginated(
      shopId,
      this.currentPage,
      this.pageSize,
      this.filterAction,
      this.filterEntityType,
      this.startDate,
      this.endDate
    ).subscribe({
      next: (response: any) => {
        this.auditLogs = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading audit logs:', error);
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadAuditLogs();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadAuditLogs();
  }

  viewDetails(log: AuditLog): void {
    this.selectedLog = log;
    this.showDetailModal = true;
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedLog = null;
  }

  parseJsonValue(value: string | null): any {
    if (!value) return null;
    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  }

  formatJsonValue(value: any): string {
    if (!value) return 'N/A';
    if (typeof value === 'string') {
      try {
        return JSON.stringify(JSON.parse(value), null, 2);
      } catch {
        return value;
      }
    }
    return JSON.stringify(value, null, 2);
  }

  getActionBadgeColor(action: string): string {
    if (action.includes('CREATED')) return 'bg-green-100 text-green-800';
    if (action.includes('UPDATED') || action.includes('ADJUSTED')) return 'bg-blue-100 text-blue-800';
    if (action.includes('DELETED') || action.includes('REVERSAL')) return 'bg-red-100 text-red-800';
    if (action.includes('DEACTIVATED')) return 'bg-yellow-100 text-yellow-800';
    if (action.includes('ACTIVATED')) return 'bg-green-100 text-green-800';
    return 'bg-gray-100 text-gray-800';
  }

  getActionIcon(action: string): string {
    if (action.includes('CREATED')) return 'fa-plus-circle';
    if (action.includes('UPDATED') || action.includes('ADJUSTED')) return 'fa-edit';
    if (action.includes('DELETED')) return 'fa-trash';
    if (action.includes('REVERSAL')) return 'fa-undo';
    if (action.includes('DEACTIVATED')) return 'fa-ban';
    if (action.includes('ACTIVATED')) return 'fa-check-circle';
    return 'fa-info-circle';
  }

  formatDate(date: Date): string {
    return this.timezoneService.formatDateInIndiaTimezone(date, {});
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
