import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, SignupRequest } from '../models/auth.model';
import { environment } from '../../environments/environment.development';
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl || 'http://localhost:8083/api';
  private currentUserSubject: BehaviorSubject<AuthResponse | null>;
  public currentUser: Observable<AuthResponse | null>;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('duebook-current-user');
    this.currentUserSubject = new BehaviorSubject<AuthResponse | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  public get token(): string | null {
    return this.currentUserValue?.token || null;
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials)
      .pipe(
        tap(response => this.setUserData(response))
      );
  }

  signup(userData: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/signup`, userData)
      .pipe(
        tap(response => this.setUserData(response))
      );
  }

  private setUserData(response: AuthResponse): void {
    localStorage.setItem('duebook-current-user', JSON.stringify(response));
    localStorage.setItem('duebook-token', response.token);
    this.currentUserSubject.next(response);
  }

  logout(): void {
    localStorage.removeItem('duebook-current-user');
    localStorage.removeItem('duebook-token');
    this.currentUserSubject.next(null);
  }

  requestOtp(email: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/request-otp`, { email });
  }

  verifyOtp(data: { email: string, otp: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/verify-otp`, data)
      .pipe(
        tap(response => {
          localStorage.setItem('duebook-current-user', JSON.stringify(response));
          localStorage.setItem('duebook-token', response.token);
          this.currentUserSubject.next(response);
        })
      );
  }

  isAuthenticated(): boolean {
    return !!this.currentUserValue && !!this.token;
  }
}
