import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile } from '../models/user.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = environment.apiUrl || 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/user/profile`);
  }

  updateBasicInfo(request: { name: string, email?: string, phone?: string }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/basic`, request);
  }

  requestOtpForPrimaryEmailChange(newEmail: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/user/profile/request-otp-for-primary-email`,
      { newEmail }, { responseType: 'text' as const });
  }
  updateBasicInfoWithOtp(request: { name: string, email?: string, phone?: string, otp?: string }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/basic-with-otp`, request);
  }

}
