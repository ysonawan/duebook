import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
    selector: 'app-signup',
    templateUrl: './signup.component.html',
    styleUrls: ['./signup.component.css'],
    standalone: false
})
export class SignupComponent {
  signupForm: FormGroup;
  loading = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }

    // Use arrow function for validator to preserve 'this' context
    this.signupForm = this.formBuilder.group({
      name: ['', Validators.required],
      phone: ['', [Validators.required, Validators.maxLength(10)]],
      email: ['', [Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: (form: FormGroup) => {
        const password = form.get('password');
        const confirmPassword = form.get('confirmPassword');
        if (password && confirmPassword && password.value !== confirmPassword.value) {
          confirmPassword.setErrors({ passwordMismatch: true });
        } else {
          // Only clear passwordMismatch error, preserve other errors
          if (confirmPassword?.hasError('passwordMismatch')) {
            const errors = { ...confirmPassword.errors };
            delete errors['passwordMismatch'];
            if (Object.keys(errors).length === 0) {
              confirmPassword.setErrors(null);
            } else {
              confirmPassword.setErrors(errors);
            }
          }
        }
        return null;
      }
    });
  }

  get f() {
    return this.signupForm.controls;
  }

  onSubmit(): void {
    if (this.signupForm.invalid) {
      // Mark all controls as touched to show validation errors
      Object.values(this.signupForm.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.loading = true;

    const { name, phone, email, password } = this.signupForm.value;

    this.authService.signup({ name, phone, email, password }).subscribe({
      next: () => {
        this.notificationService.success('Signup successful! Welcome to DueBook.', 'Success');
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        // Error is handled by HttpErrorInterceptor
        this.loading = false;
      }
    });
  }
}


