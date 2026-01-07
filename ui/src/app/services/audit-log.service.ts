import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../environments/environment.development";

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private apiUrl = environment.apiUrl || 'http://localhost:8083/api';

  constructor(private http: HttpClient) {}

  /**
   * Get paginated audit logs for a shop with filters and date range
   */
  getAuditLogsPaginated(
    shopId: number,
    page: number = 0,
    size: number = 20,
    action?: string,
    entityType?: string,
    startDate?: string,
    endDate?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (action && action.trim()) {
      params = params.set('action', action);
    }

    if (entityType && entityType.trim()) {
      params = params.set('entityType', entityType);
    }

    if (startDate && startDate.trim()) {
      params = params.set('startDate', startDate);
    }

    if (endDate && endDate.trim()) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<any>(`${this.apiUrl}/audit-logs/shop/${shopId}/paginated`, { params });
  }

  /**
   * Get distinct actions for a shop
   */
  getDistinctActions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/audit-logs/actions`);
  }

  /**
   * Get distinct entity types for a shop
   */
  getDistinctEntityTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/audit-logs/entity-types`);
  }
}

