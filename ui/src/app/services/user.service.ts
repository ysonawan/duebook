import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserProfile, UpdateSecondaryEmailsRequest } from '../models/user.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = environment.apiUrl || 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/user/profile`).pipe(
      catchError(this.handleError)
    );
  }

  updateSecondaryEmails(request: UpdateSecondaryEmailsRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/secondary-emails`, request).pipe(
      catchError(this.handleError)
    );
  }

  updateBasicInfo(request: { name: string, email: string }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/basic`, request).pipe(
      catchError(this.handleError)
    );
  }

  sendPortfolioReport(): Observable<string> {
    return this.http.post(`${this.apiUrl}/user/profile/send-portfolio-report`, {}, { responseType: 'text' as const }).pipe(
      catchError(this.handleError)
    );
  }

  sendBudgetReport(): Observable<string> {
    return this.http.post(`${this.apiUrl}/user/profile/send-budget-report`, {}, { responseType: 'text' as const }).pipe(
      catchError(this.handleError)
    );
  }

  requestOtpForPrimaryEmailChange(newEmail: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/user/profile/request-otp-for-primary-email`,
      { newEmail }, { responseType: 'text' as const }).pipe(
      catchError(this.handleError)
    );
  }

  requestOtpForSecondaryEmailChange(secondaryEmails: string[]): Observable<string> {
    return this.http.post(`${this.apiUrl}/user/profile/request-otp-for-secondary-emails`,
      { secondaryEmails }, { responseType: 'text' as const }).pipe(
      catchError(this.handleError)
    );
  }

  updateSecondaryEmailsWithOtp(request: { secondaryEmails: string[], otp: string }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/secondary-emails-with-otp`, request).pipe(
      catchError(this.handleError)
    );
  }

  updateBasicInfoWithOtp(request: { name: string, email?: string, otp?: string }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user/profile/basic-with-otp`, request).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any) {
    let errorMsg = 'An unknown error occurred.';
    if (error.error && error.error.message) {
      errorMsg = error.error.message;
    } else if (error.message) {
      errorMsg = error.message;
    }
    return throwError(() => ({ ...error, error: { ...error.error, message: errorMsg } }));
  }
}
