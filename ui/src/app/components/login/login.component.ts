import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    standalone: false
})
export class LoginComponent {
  loginForm: FormGroup;
  otpForm: FormGroup;
  loading = false;
  sessionExpiredMessage = '';
  returnUrl: string;
  loginMode: 'password' | 'otp' = 'password';
  otpSent = false;
  otpSending = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }

    this.loginForm = this.formBuilder.group({
      phone: ['', [Validators.required, Validators.maxLength(10)]],
      password: ['', Validators.required]
    });

    this.otpForm = this.formBuilder.group({
      phone: ['', [Validators.required, Validators.maxLength(10)]],
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';

    // Check if user was redirected due to session expiration
    if (this.route.snapshot.queryParams['sessionExpired'] === 'true') {
      this.sessionExpiredMessage = 'Your session has expired. Please login again.';
    }
  }

  get f() {
    return this.loginForm.controls;
  }

  get otpF() {
    return this.otpForm.controls;
  }

  switchLoginMode(mode: 'password' | 'otp'): void {
    this.loginMode = mode;
    this.sessionExpiredMessage = '';
    this.otpSent = false;

    // Reset forms
    this.loginForm.reset();
    this.otpForm.reset();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      // Mark all controls as touched to show validation errors
      Object.values(this.loginForm.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.sessionExpiredMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate([this.returnUrl]);
      },
      error: () => {
        // Error is handled by HttpErrorInterceptor
        this.loading = false;
      }
    });
  }

  requestOtp(): void {
    const email = this.otpForm.get('email')?.value;
    if (!email || this.otpForm.get('email')?.invalid) {
      this.otpForm.get('email')?.markAsTouched();
      return;
    }

    this.otpSending = true;

    this.authService.requestOtp(email).subscribe({
      next: () => {
        this.otpSent = true;
        this.otpSending = false;
        this.notificationService.success('OTP sent to your email. Please check your inbox.', 'Success');
      },
      error: () => {
        // Error is handled by HttpErrorInterceptor
        this.otpSending = false;
      }
    });
  }

  verifyOtp(): void {
    if (this.otpForm.invalid) {
      Object.keys(this.otpForm.controls).forEach(key => {
        this.otpForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;

    this.authService.verifyOtp(this.otpForm.value).subscribe({
      next: () => {
        this.notificationService.success('OTP verified successfully!', 'Success');
        this.router.navigate([this.returnUrl]);
      },
      error: () => {
        // Error is handled by HttpErrorInterceptor
        this.loading = false;
      }
    });
  }
}


