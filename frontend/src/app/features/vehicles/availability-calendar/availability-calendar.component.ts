import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatCalendar,
  MatCalendarCellClassFunction,
  MatDatepickerModule,
} from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AvailabilityResponse } from '../models/vehicle.model';
import { AvailabilityService } from '../services/availability.service';
import { setupFallbackTranslations } from '../vehicles.constants';

export interface DateRangeSelection {
  start: Date | null;
  end: Date | null;
}

@Component({
  selector: 'app-availability-calendar',
  standalone: true,
  imports: [
    CommonModule,
    MatCalendar,
    MatDatepickerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './availability-calendar.component.html',
  styleUrl: './availability-calendar.component.scss',
})
export class AvailabilityCalendarComponent implements OnInit, OnChanges {
  private readonly translate = inject(TranslateService, { optional: true });
  private readonly availabilityService = inject(AvailabilityService, { optional: true });

  @Input() vehicleId?: number;
  @Input() availability?: AvailabilityResponse;
  @Input() selectable = true;
  @Input() rangeMode = true;

  @Output() dateRangeChange = new EventEmitter<DateRangeSelection>();
  @Output() dateSelected = new EventEmitter<Date | null>();

  selectedRange: DateRangeSelection = { start: null, end: null };
  selectedDate: Date | null = new Date();
  hoveredDate: Date | null = null;
  minDate = new Date();

  activeAvailability?: AvailabilityResponse;
  selectedStatus: 'AVAILABLE' | 'UNAVAILABLE' | 'PARTIAL' | null = null;
  selectedTooltip = '';

  constructor() {
    setupFallbackTranslations(this.translate);
    this.minDate.setHours(0, 0, 0, 0);
  }

  ngOnInit(): void {
    if (this.availability) {
      this.activeAvailability = this.availability;
      this.evaluateStatus(this.selectedDate);
    } else if (this.vehicleId) {
      this.loadAvailability();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['availability'] && this.availability) {
      this.activeAvailability = this.availability;
      this.evaluateStatus(this.selectedDate);
    } else if (changes['vehicleId'] && this.vehicleId && !this.availability) {
      this.loadAvailability();
    }
  }

  loadAvailability(): void {
    if (!this.vehicleId || !this.availabilityService) return;
    this.availabilityService.getAvailability(this.vehicleId).subscribe({
      next: (res) => {
        this.activeAvailability = res;
        this.evaluateStatus(this.selectedDate);
      },
    });
  }

  dateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    const cellDate = new Date(date);
    cellDate.setHours(0, 0, 0, 0);

    // Blocage des dates passées
    if (cellDate < this.minDate) {
      return false;
    }

    // Jours indisponibles non sélectionnables
    const iso = this.formatDateIso(cellDate);
    if (this.activeAvailability?.unavailable) {
      const isUnavailable = this.activeAvailability.unavailable.some(
        (u) => iso >= u.from && iso <= u.to,
      );
      if (isUnavailable) {
        return false;
      }
    }

    return true;
  };

  dateClass: MatCalendarCellClassFunction<Date> = (cellDate: Date, view: string) => {
    if (view !== 'month') return '';

    const dateOnly = new Date(cellDate);
    dateOnly.setHours(0, 0, 0, 0);
    const isoDate = this.formatDateIso(dateOnly);

    // Dates passées
    if (dateOnly < this.minDate) {
      return 'calendar-date-past';
    }

    // Période indisponible : rouge
    if (this.activeAvailability?.unavailable) {
      for (const p of this.activeAvailability.unavailable) {
        if (isoDate >= p.from && isoDate <= p.to) {
          return 'calendar-date-unavailable';
        }
      }
    }

    // Vérifier les jours de transition / partiellement disponibles (ex: retour le matin, départ l'après-midi)
    if (this.activeAvailability?.unavailable) {
      const isBoundary = this.activeAvailability.unavailable.some(
        (p) => isoDate === p.from || isoDate === p.to,
      );
      if (isBoundary) {
        return 'calendar-date-partial';
      }
    }

    // Plage sélectionnée en cours
    if (this.selectedRange.start && this.selectedRange.end) {
      const startIso = this.formatDateIso(this.selectedRange.start);
      const endIso = this.formatDateIso(this.selectedRange.end);
      if (isoDate >= startIso && isoDate <= endIso) {
        return 'calendar-date-selected-range';
      }
    } else if (this.selectedRange.start && isoDate === this.formatDateIso(this.selectedRange.start)) {
      return 'calendar-date-selected-start';
    }

    // Par défaut disponible : fond blanc
    return 'calendar-date-available';
  };

  onDateSelected(date: Date | null): void {
    if (!date || !this.selectable) return;

    if (!this.rangeMode) {
      this.selectedDate = date;
      this.dateSelected.emit(date);
      this.evaluateStatus(date);
      return;
    }

    // Sélection d'une plage (dateDebut -> dateFin)
    if (!this.selectedRange.start || (this.selectedRange.start && this.selectedRange.end)) {
      this.selectedRange = { start: date, end: null };
      this.selectedDate = date;
    } else if (this.selectedRange.start && !this.selectedRange.end) {
      if (date < this.selectedRange.start) {
        this.selectedRange = { start: date, end: null };
      } else {
        // Vérifier si la plage sélectionnée contient un jour indisponible
        const hasUnavailableInRange = this.checkRangeHasUnavailable(this.selectedRange.start, date);
        if (hasUnavailableInRange) {
          // Si la plage traverse une indisponibilité, on reset au nouveau début
          this.selectedRange = { start: date, end: null };
        } else {
          this.selectedRange = { start: this.selectedRange.start, end: date };
        }
      }
    }

    this.dateRangeChange.emit(this.selectedRange);
    this.evaluateStatus(date);
  }

  private checkRangeHasUnavailable(start: Date, end: Date): boolean {
    if (!this.activeAvailability?.unavailable?.length) return false;

    const startIso = this.formatDateIso(start);
    const endIso = this.formatDateIso(end);

    return this.activeAvailability.unavailable.some(
      (p) => (startIso <= p.to && endIso >= p.from),
    );
  }

  evaluateStatus(date: Date | null): void {
    if (!date) {
      this.selectedStatus = null;
      this.selectedTooltip = '';
      return;
    }

    const isoDate = this.formatDateIso(date);
    if (this.activeAvailability?.unavailable) {
      const unavail = this.activeAvailability.unavailable.find(
        (p) => isoDate >= p.from && isoDate <= p.to,
      );
      if (unavail) {
        this.selectedStatus = 'UNAVAILABLE';
        this.selectedTooltip = `Indisponible (réservé du ${unavail.from} au ${unavail.to})`;
        return;
      }
    }

    this.selectedStatus = 'AVAILABLE';
    this.selectedTooltip = 'Véhicule disponible à cette date';
  }

  private formatDateIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
