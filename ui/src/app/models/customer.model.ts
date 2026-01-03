import { Shop } from './shop.model';

export interface Customer {
  id?: number;
  name: string;
  entityName: string;
  phone: string;
  openingBalance: number;
  currentBalance: number;
  isActive?: boolean;
  shop?: Shop;
  shopId?: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface CustomerResponse extends Customer {
  shop: Shop;
}

