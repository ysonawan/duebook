import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Shop } from '../models/shop.model';
import { ShopUser } from '../models/shop-user.model';

@Injectable({
  providedIn: 'root'
})
export class ShopService {
  private apiUrl = '/api/shops';

  constructor(private http: HttpClient) {}

  getAllShops(): Observable<Shop[]> {
    return this.http.get<Shop[]>(this.apiUrl);
  }

  getShopById(id: number): Observable<Shop> {
    return this.http.get<Shop>(`${this.apiUrl}/${id}`);
  }

  createShop(shop: Shop): Observable<Shop> {
    return this.http.post<Shop>(this.apiUrl, shop);
  }

  updateShop(id: number, shop: Shop): Observable<Shop> {
    return this.http.put<Shop>(`${this.apiUrl}/${id}`, shop);
  }

  deleteShop(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Shop User Management Methods
  getShopUsers(shopId: number): Observable<ShopUser[]> {
    return this.http.get<ShopUser[]>(`${this.apiUrl}/${shopId}/users`);
  }

  addUserToShop(shopId: number, shopUser: ShopUser): Observable<ShopUser> {
    return this.http.post<ShopUser>(`${this.apiUrl}/${shopId}/users`, shopUser);
  }

  updateUserRole(shopId: number, shopUserId: number, role: string): Observable<ShopUser> {
    return this.http.put<ShopUser>(`${this.apiUrl}/${shopId}/users/${shopUserId}/role`, { role });
  }

  removeUserFromShop(shopId: number, shopUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${shopId}/users/${shopUserId}`);
  }
}

