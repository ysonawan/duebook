export interface ShopUser {
  id?: number;
  shopId: number;
  userId?: number;
  userPhone: string;
  userName?: string;
  userEmail?: string;
  role: 'OWNER' | 'STAFF' | 'VIEWER';
  status?: 'ACTIVE' | 'INVITED' | 'INACTIVE';
  joinedAt?: Date;
}

export interface ShopUserRole {
  id: string;
  label: string;
  description: string;
  icon: string;
}

