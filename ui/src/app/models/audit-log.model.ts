export interface AuditLog {
  id: number;
  shopId: number;
  entityType: string;
  entityId: string | number;
  action: string;
  performedById: number;
  performedByName: string;
  oldValue: string | null;
  newValue: string | null;
  performedAt: Date;
}

