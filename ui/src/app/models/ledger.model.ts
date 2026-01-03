export enum LedgerEntryType {
  BAKI = 'BAKI',
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
  entryDate: Date;
  createdAt: Date;
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

