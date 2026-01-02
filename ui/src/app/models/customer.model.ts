import { Shop } from './shop.model';

export interface Customer {
  id?: number;
  name: string;
  phone: string;
  openingBalance: number;
  currentBalance: number;
  isActive?: boolean;
  shop?: Shop;
  shopId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CustomerResponse extends Customer {
  shop: Shop;
}

