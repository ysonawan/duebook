import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private apiUrl = '/api/customers';

  constructor(private http: HttpClient) {}

  getAllCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl);
  }

  getCustomerById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/${id}`);
  }

  createCustomer(customer: Customer): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, customer);
  }

  updateCustomer(id: number, customer: Customer): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/${id}`, customer);
  }

  getCustomersByShop(shopId: number): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.apiUrl}/shops/${shopId}/active`);
  }

  /**
   * Get paginated customers for a shop with filters and search
   */
  getCustomersPaginated(
    shopId: number,
    page: number = 0,
    size: number = 20,
    status?: string,
    searchTerm?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status && status.trim()) {
      params = params.set('status', status);
    }

    if (searchTerm && searchTerm.trim()) {
      params = params.set('searchTerm', searchTerm);
    }

    return this.http.get<any>(`${this.apiUrl}/shop/${shopId}/paginated`, { params });
  }

  /**
   * Get customer summary with filters (all records, not paginated)
   * Used for summary cards that need complete data across all pages
   */
  getCustomerSummary(
    shopId: number,
    status?: string,
    searchTerm?: string
  ): Observable<any> {
    let params = new HttpParams();

    if (status && status.trim()) {
      params = params.set('status', status);
    }

    if (searchTerm && searchTerm.trim()) {
      params = params.set('searchTerm', searchTerm);
    }

    return this.http.get<any>(`${this.apiUrl}/shop/${shopId}/summary`, { params });
  }
}
