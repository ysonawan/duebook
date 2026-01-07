import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Shop } from '../models/shop.model';
import { ShopUser } from '../models/shop-user.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class ShopService {
  private apiUrl = environment.apiUrl || 'http://localhost:8083/api';

  constructor(private http: HttpClient) {}

  getAllShops(): Observable<Shop[]> {
    return this.http.get<Shop[]>(`${this.apiUrl}/shops`);
  }

  getShopById(id: number): Observable<Shop> {
    return this.http.get<Shop>(`${this.apiUrl}/shops/${id}`);
  }

  createShop(shop: Shop): Observable<Shop> {
    return this.http.post<Shop>(`${this.apiUrl}/shops`, shop);
  }

  updateShop(id: number, shop: Shop): Observable<Shop> {
    return this.http.put<Shop>(`${this.apiUrl}/shops/${id}`, shop);
  }

  deleteShop(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/shops/${id}`);
  }

  // Shop User Management Methods
  getShopUsers(shopId: number): Observable<ShopUser[]> {
    return this.http.get<ShopUser[]>(`${this.apiUrl}/shops/${shopId}/users`);
  }

  addUserToShop(shopId: number, shopUser: ShopUser): Observable<ShopUser> {
    return this.http.post<ShopUser>(`${this.apiUrl}/shops/${shopId}/users`, shopUser);
  }

  updateUserRole(shopId: number, shopUserId: number, role: string): Observable<ShopUser> {
    return this.http.put<ShopUser>(`${this.apiUrl}/shops/${shopId}/users/${shopUserId}/role`, { role });
  }

  removeUserFromShop(shopId: number, shopUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/shops/${shopId}/users/${shopUserId}`);
  }
}

