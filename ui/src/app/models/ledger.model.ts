export enum LedgerEntryType {
  JAMA = 'JAMA',
  PAID = 'PAID',
  REVERSAL = 'REVERSAL'
}

export interface CustomerLedger {
  id?: number;
  customerId: number;
  shopId?: number;
  entryType: LedgerEntryType;
  amount: number;
  balanceAfter: number;
  referenceEntryId?: number;
  notes?: string;
  entryDate: string;
  createdAt?: string;
  createdByUser?: {
    id: number;
    name: string;
    email: string;
  };
  customer?: {
    id: number;
    name: string;
    phone: string;
  };
}

export interface CustomerLedgerResponse extends CustomerLedger {
  customer: {
    id: number;
    name: string;
    phone: string;
  };
  createdByUser: {
    id: number;
    name: string;
    email: string;
  };
}

