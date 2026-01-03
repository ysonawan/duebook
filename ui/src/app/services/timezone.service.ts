import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

/**
 * Converts a UTC timestamp to India timezone (IST - UTC+5:30)
 * In production: timestamps are stored as UTC and need to be converted to IST
 * In development: timestamps are already in IST timezone
 *
 */
@Injectable({
  providedIn: 'root'
})
export class TimezoneService {
  // India timezone offset
  private readonly INDIA_TIMEZONE = 'Asia/Kolkata';

  constructor() {}

  formatDateInIndiaTimezone(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    const dateObj = new Date(date);

    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: this.INDIA_TIMEZONE,
      ...options
    };

    return dateObj.toLocaleDateString('en-IN', defaultOptions);
  }

  formatDateOnlyInIndiaTimezone(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    const dateObj = new Date(date);

    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      timeZone: this.INDIA_TIMEZONE,
      ...options
    };

    return dateObj.toLocaleDateString('en-IN', defaultOptions);
  }

  getTimezoneAbbreviation(): string {
    return 'IST (UTC+5:30)';
  }
}

