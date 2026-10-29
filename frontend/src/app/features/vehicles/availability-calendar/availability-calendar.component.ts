import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatCalendar,
  MatCalendarCellClassFunction,
  MatDatepickerModule,
} from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AvailabilityResponse } from '../models/vehicle.model';
import { setupFallbackTranslations } from '../vehicles.constants';

@Component({
  selector: 'app-availability-calendar',
  standalone: true,
  imports: [CommonModule, MatCalendar, MatDatepickerModule, TranslateModule],
  providers: [provideNativeDateAdapter()],
  templateUrl: './availability-calendar.component.html',
  styleUrl: './availability-calendar.component.scss',
})
export class AvailabilityCalendarComponent implements OnChanges {
  private readonly translate = inject(TranslateService, { optional: true });

  @Input() vehicleId?: number;
  @Input() availability?: AvailabilityResponse;

  selectedDate: Date | null = new Date();
  selectedStatus: 'AVAILABLE' | 'UNAVAILABLE' | null = null;

  constructor() {
    setupFallbackTranslations(this.translate);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['availability'] || changes['vehicleId']) {
      this.evaluateSelectedDateStatus(this.selectedDate);
    }
  }

  dateClass: MatCalendarCellClassFunction<Date> = (cellDate: Date, view: string) => {
    if (view !== 'month') {
      return '';
    }

    const isoDate = this.formatDateIso(cellDate);

    // Check if cell falls in unavailable period
    if (this.availability?.unavailable) {
      for (const period of this.availability.unavailable) {
        if (isoDate >= period.from && isoDate <= period.to) {
          return 'calendar-date-unavailable';
        }
      }
    }

    // Check if cell falls in explicitly available period
    if (this.availability?.available && this.availability.available.length > 0) {
      for (const period of this.availability.available) {
        if (isoDate >= period.from && isoDate <= period.to) {
          return 'calendar-date-available';
        }
      }
    }

    // Default: dates in the future are generally available
    const today = this.formatDateIso(new Date());
    if (isoDate >= today) {
      return 'calendar-date-available';
    }

    return 'calendar-date-past';
  };

  onDateSelected(date: Date | null): void {
    this.selectedDate = date;
    this.evaluateSelectedDateStatus(date);
  }

  evaluateSelectedDateStatus(date: Date | null): void {
    if (!date) {
      this.selectedStatus = null;
      return;
    }

    const isoDate = this.formatDateIso(date);

    if (this.availability?.unavailable) {
      const isUnavailable = this.availability.unavailable.some(
        (p) => isoDate >= p.from && isoDate <= p.to,
      );
      if (isUnavailable) {
        this.selectedStatus = 'UNAVAILABLE';
        return;
      }
    }

    this.selectedStatus = 'AVAILABLE';
  }

  private formatDateIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
