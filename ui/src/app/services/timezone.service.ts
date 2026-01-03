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
  private readonly IST_OFFSET_MINUTES = 330; // UTC+5:30 in minutes

  constructor() {}

  /**
   * Converts UTC timestamp to India timezone and formats it.
   * Explicitly treats incoming timestamp as UTC to ensure proper conversion in production.
   * @param date - UTC timestamp from database (can be string or Date)
   * @param options - Custom formatting options
   * @returns Formatted date string in IST
   */
  formatDateInIndiaTimezone(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    // Parse the input as UTC
    const dateObj = new Date(date);

    // Get the UTC time in milliseconds
    const utcTime = dateObj.getTime();

    // Calculate IST time by adding the offset (only in production where we receive UTC)
    // In production, the browser may be in any timezone, so we need to explicitly convert
    const istTime = environment.production
      ? new Date(utcTime + (this.IST_OFFSET_MINUTES * 60 * 1000))
      : dateObj;

    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: this.INDIA_TIMEZONE,
      ...options
    };

    return istTime.toLocaleDateString('en-IN', defaultOptions);
  }

  /**
   * Converts UTC timestamp to India timezone and formats date only (no time).
   * Explicitly treats incoming timestamp as UTC to ensure proper conversion in production.
   * @param date - UTC timestamp from database (can be string or Date)
   * @param options - Custom formatting options
   * @returns Formatted date string in IST (date only)
   */
  formatDateOnlyInIndiaTimezone(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    // Parse the input as UTC
    const dateObj = new Date(date);

    // Get the UTC time in milliseconds
    const utcTime = dateObj.getTime();

    // Calculate IST time by adding the offset (only in production where we receive UTC)
    // In production, the browser may be in any timezone, so we need to explicitly convert
    const istTime = environment.production
      ? new Date(utcTime + (this.IST_OFFSET_MINUTES * 60 * 1000))
      : dateObj;

    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      timeZone: this.INDIA_TIMEZONE,
      ...options
    };

    return istTime.toLocaleDateString('en-IN', defaultOptions);
  }

  /**
   * Get the timezone abbreviation
   * @returns IST timezone abbreviation with offset
   */
  getTimezoneAbbreviation(): string {
    return 'IST (UTC+5:30)';
  }
}

