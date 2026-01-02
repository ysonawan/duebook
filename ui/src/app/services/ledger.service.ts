import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomerLedger } from '../models/ledger.model';

@Injectable({
  providedIn: 'root'
})
export class LedgerService {
  private apiUrl = '/api/ledger';

  constructor(private http: HttpClient) {}

  getAllLedgerEntries(): Observable<CustomerLedger[]> {
    return this.http.get<CustomerLedger[]>(this.apiUrl);
  }

  getLedgerById(id: number): Observable<CustomerLedger> {
    return this.http.get<CustomerLedger>(`${this.apiUrl}/${id}`);
  }

  getLedgerByCustomerId(customerId: number): Observable<CustomerLedger[]> {
    return this.http.get<CustomerLedger[]>(`${this.apiUrl}/customer/${customerId}`);
  }

  createLedgerEntry(entry: CustomerLedger): Observable<CustomerLedger> {
    return this.http.post<CustomerLedger>(this.apiUrl, entry);
  }

  reverseLedgerEntry(entryId: number, notes?: string): Observable<CustomerLedger> {
    return this.http.post<CustomerLedger>(`${this.apiUrl}/${entryId}/reverse`, { notes });
  }

  getLedgerByShop(shopId: number): Observable<CustomerLedger[]> {
    return this.http.get<CustomerLedger[]>(`${this.apiUrl}/shop/${shopId}`);
  }

  getLedgerByDateRange(startDate: string, endDate: string): Observable<CustomerLedger[]> {
    return this.http.get<CustomerLedger[]>(`${this.apiUrl}/date-range`, {
      params: { startDate, endDate }
    });
  }
}

