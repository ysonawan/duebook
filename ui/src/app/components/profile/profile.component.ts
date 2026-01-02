import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { UserProfile } from '../../models/user.model';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.css'],
    standalone: false
})
export class ProfileComponent implements OnInit {
  profile: UserProfile | null = null;
  newSecondaryEmail: string = '';
  loading = false;
  saving = false;
  sendingReport = false;
  sendingBudgetReport = false;
  editingBasic = false;
  basicForm = {
    name: '',
    email: ''
  };

  // OTP Modal state
  showOtpModal = false;
  otpModalType: 'primary' | 'secondary' | null = null;
  otpCode = '';
  otpLoading = false;
  otpSent = false;
  otpVerificationInProgress = false;
  newPrimaryEmail = '';
  pendingSecondaryEmail: string = '';

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
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


  addSingleSecondaryEmail(): void {
    const email = this.newSecondaryEmail.trim();

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      this.showMessage('error', 'Invalid email format');
      return;
    }

    // Check if email is same as primary
    if (this.profile && email.toLowerCase() === this.profile.email.toLowerCase()) {
      this.showMessage('error', 'Secondary email cannot be the same as primary email');
      return;
    }

    // Check if email already exists in secondary emails
    if (this.profile && this.profile.secondaryEmails &&
        this.profile.secondaryEmails.some(e => e.toLowerCase() === email.toLowerCase())) {
      this.showMessage('error', 'This email is already added');
      return;
    }

    // Store pending email and show OTP modal
    this.pendingSecondaryEmail = email;
    this.otpModalType = 'secondary';
    this.showOtpModal = true;
    this.otpCode = '';
    this.otpSent = false;
    this.requestOtpForSecondaryEmails();
  }

  removeSecondaryEmail(index: number): void {
    if (!this.profile) return;

    const updatedEmails = this.profile.secondaryEmails.filter((_, i) => i !== index);

    this.saving = true;
    this.userService.updateSecondaryEmailsWithOtp({
      secondaryEmails: updatedEmails,
      otp: '' // No OTP needed for removal
    }).subscribe({
      next: (updatedProfile) => {
        this.profile = updatedProfile;
        this.saving = false;
        this.showMessage('success', `Email removed successfully`);
      },
      error: (error) => {
        this.saving = false;
        this.showMessage('error', error?.error?.message || 'Failed to remove email');
      }
    });
  }

  requestOtpForSecondaryEmails(): void {
    this.otpLoading = true;
    this.userService.requestOtpForSecondaryEmailChange([this.pendingSecondaryEmail]).subscribe({
      next: (message) => {
        this.otpLoading = false;
        this.otpSent = true;
        this.showMessage('success', message || 'OTP sent to your newly added email');
      },
      error: (error) => {
        this.otpLoading = false;
        this.showMessage('error', error?.error?.message || 'Failed to send OTP');
        this.closeOtpModal();
      }
    });
  }

  verifyOtpForSecondaryEmails(): void {
    if (!this.otpCode || this.otpCode.trim().length === 0) {
      this.showMessage('error', 'Please enter the OTP');
      return;
    }

    // Build the complete secondary emails list with the new email
    const updatedEmails = this.profile?.secondaryEmails ? [...this.profile.secondaryEmails] : [];
    updatedEmails.push(this.pendingSecondaryEmail);

    this.otpVerificationInProgress = true;
    this.userService.updateSecondaryEmailsWithOtp({
      secondaryEmails: updatedEmails,
      otp: this.otpCode.trim()
    }).subscribe({
      next: (updatedProfile) => {
        this.profile = updatedProfile;
        this.otpVerificationInProgress = false;
        this.closeOtpModal();
        this.newSecondaryEmail = '';
        this.showMessage('success', 'Secondary email added successfully');
      },
      error: (error) => {
        this.otpVerificationInProgress = false;
        this.showMessage('error', error?.error?.message || 'OTP verification failed. Please try again.');
      }
    });
  }

  showMessage(type: 'success' | 'error', text: string): void {
    if (type === 'success') {
      this.toastr.success(text);
    } else {
      this.toastr.error(text);
    }
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
        this.showMessage('error', error?.error?.message || 'Failed to send portfolio report. Please try again.');
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
        this.showMessage('error', error?.error?.message || 'Failed to send budget report. Please try again.');
      }
    });
  }


  startEditBasic() {
    if (this.profile) {
      this.basicForm.name = this.profile.name;
      this.basicForm.email = this.profile.email;
      this.editingBasic = true;
    }
  }

  saveBasicInfo() {
    // Check if email has changed
    if (this.basicForm.email !== this.profile?.email) {
      // Email is changing, need OTP verification
      this.newPrimaryEmail = this.basicForm.email;
      this.otpModalType = 'primary';
      this.showOtpModal = true;
      this.otpCode = '';
      this.otpSent = false;
      this.requestOtpForPrimaryEmail();
    } else {
      // Only name is changing, no OTP needed
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
      error: (error) => {
        this.otpLoading = false;
        this.showMessage('error', error?.error?.message || 'Failed to send OTP to new email address');
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
      error: (err) => {
        this.otpVerificationInProgress = false;
        this.showMessage('error', err?.error?.message || 'OTP verification failed. Please try again.');
      }
    });
  }

  updateBasicInfoWithoutEmailChange(): void {
    this.saving = true;
    this.userService.updateBasicInfo({
      name: this.basicForm.name,
      email: this.profile?.email || ''
    }).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.editingBasic = false;
        this.saving = false;
        this.showMessage('success', 'Profile updated successfully');
      },
      error: (err) => {
        this.saving = false;
        this.showMessage('error', err?.error?.message || 'Failed to update profile');
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
    this.pendingSecondaryEmail = '';
  }
}
