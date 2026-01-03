import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { UserProfile } from '../../models/user.model';
import { Router } from '@angular/router';
import {NotificationService} from "../../services/notification.service";

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.css'],
    standalone: false
})
export class ProfileComponent implements OnInit {
  profile: UserProfile | null = null;
  loading = false;
  saving = false;
  sendingReport = false;
  sendingBudgetReport = false;
  editingBasic = false;
  basicForm = {
    name: '',
    email: '',
    phone: ''
  };

  // OTP Modal state
  showOtpModal = false;
  otpModalType: 'primary' | 'secondary' | null = null;
  otpCode = '';
  otpLoading = false;
  otpSent = false;
  otpVerificationInProgress = false;
  newPrimaryEmail = '';
  previousEmail = '';

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  showMessage(type: 'success' | 'error', text: string): void {
    if (type === 'success') {
      this.notification.success(text, 'Success');
    } else {
      this.notification.error(text, 'Error');
    }
  }


  loadProfile(): void {
    this.loading = true;
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        this.loading = false;
        this.showMessage('error', 'Failed to load profile');
      }
    });
  }

  sendReport(): void {
    this.sendingReport = true;
    this.userService.sendPortfolioReport().subscribe({
      next: () => {
        this.sendingReport = false;
        this.showMessage('success', 'Portfolio report has been queued and will be sent to all your email addresses shortly.');
      },
      error: (error) => {
        console.error('Error sending report:', error);
        this.sendingReport = false;
      }
    });
  }

  sendBudgetReport(): void {
    this.sendingBudgetReport = true;
    this.userService.sendBudgetReport().subscribe({
      next: () => {
        this.sendingBudgetReport = false;
        this.showMessage('success', 'Budget report has been queued and will be sent to all your email addresses shortly.');
      },
      error: (error) => {
        console.error('Error sending budget report:', error);
        this.sendingBudgetReport = false;
      }
    });
  }


  startEditBasic() {
    if (this.profile) {
      this.basicForm.name = this.profile.name;
      this.basicForm.email = this.profile.email || '';
      this.basicForm.phone = this.profile.phone || '';
      this.previousEmail = this.profile.email || '';
      this.editingBasic = true;
    }
  }

  saveBasicInfo() {
    // Validate mandatory fields
    if (!this.basicForm.phone || this.basicForm.phone.trim() === '') {
      this.showMessage('error', 'Phone number (username) is required');
      return;
    }

    if (!this.basicForm.name || this.basicForm.name.trim() === '') {
      this.showMessage('error', 'Full name is required');
      return;
    }

    // Check if email has changed (added or updated)
    const emailHasChanged = this.basicForm.email !== this.previousEmail;

    if (emailHasChanged && this.basicForm.email.trim() !== '') {
      // Email is being added or changed, need OTP verification
      this.newPrimaryEmail = this.basicForm.email;
      this.otpModalType = 'primary';
      this.showOtpModal = true;
      this.otpCode = '';
      this.otpSent = false;
      this.requestOtpForPrimaryEmail();
    } else {
      // No email change or email removed, no OTP needed
      this.updateBasicInfoWithoutEmailChange();
    }
  }

  requestOtpForPrimaryEmail(): void {
    this.otpLoading = true;
    this.userService.requestOtpForPrimaryEmailChange(this.newPrimaryEmail).subscribe({
      next: (message) => {
        this.otpLoading = false;
        this.otpSent = true;
        this.showMessage('success', message || 'OTP sent to your new email address');
      },
      error: () => {
        this.otpLoading = false;
        this.closeOtpModal();
      }
    });
  }

  verifyOtpForPrimaryEmail(): void {
    if (!this.otpCode || this.otpCode.trim().length === 0) {
      this.showMessage('error', 'Please enter the OTP');
      return;
    }

    this.otpVerificationInProgress = true;
    this.userService.updateBasicInfoWithOtp({
      name: this.basicForm.name,
      email: this.newPrimaryEmail,
      phone: this.basicForm.phone,
      otp: this.otpCode.trim()
    }).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.editingBasic = false;
        this.otpVerificationInProgress = false;
        this.closeOtpModal();
        this.showMessage('success', 'Primary email updated successfully. Please log in again.');
        // Logout user after email change
        setTimeout(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: () => {
        this.otpVerificationInProgress = false;
      }
    });
  }

  updateBasicInfoWithoutEmailChange(): void {
    this.saving = true;
    this.userService.updateBasicInfo({
      name: this.basicForm.name,
      email: this.basicForm.email || '',
      phone: this.basicForm.phone
    }).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.editingBasic = false;
        this.saving = false;
        this.showMessage('success', 'Profile updated successfully');
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  closeOtpModal(): void {
    this.showOtpModal = false;
    this.otpModalType = null;
    this.otpCode = '';
    this.otpSent = false;
    this.otpVerificationInProgress = false;
    this.newPrimaryEmail = '';
  }
}
