import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

  /**
   * Get paginated ledger entries for a shop with filters and date range
   */
  getLedgerEntriesPaginated(
    shopId: number,
    page: number = 0,
    size: number = 20,
    customerId?: number,
    entryType?: string,
    startDate?: string,
    endDate?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (customerId) {
      params = params.set('customerId', customerId.toString());
    }

    if (entryType && entryType.trim()) {
      params = params.set('entryType', entryType);
    }

    if (startDate && startDate.trim()) {
      params = params.set('startDate', startDate);
    }

    if (endDate && endDate.trim()) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<any>(`${this.apiUrl}/shop/${shopId}/paginated`, { params });
  }
}

