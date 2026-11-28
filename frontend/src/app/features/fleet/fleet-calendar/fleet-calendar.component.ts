import {
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { FleetVehicle } from '../../vehicles/services/fleet.service';
import { Reservation } from '../../reservations/models/reservation.model';

export interface CalendarDay {
  date: Date;
  iso: string;
  dayName: string;
  dayNumber: number;
  monthLabel: string;
  isWeekend: boolean;
  isToday: boolean;
}

export interface GanttBar {
  reservation: Reservation;
  leftPercent: number;
  widthPercent: number;
  tooltip: string;
  statusClass: string;
  label: string;
}

type RangeDays = 7 | 14 | 30;

@Component({
  selector: 'app-fleet-calendar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './fleet-calendar.component.html',
  styleUrl: './fleet-calendar.component.scss',
})
export class FleetCalendarComponent {
  private readonly vehiclesSignal = signal<FleetVehicle[]>([]);
  private readonly reservationsSignal = signal<Reservation[]>([]);
  private readonly selectedVehicleIdSignal = signal<number | null>(null);

  @Input()
  set vehicles(value: FleetVehicle[]) {
    this.vehiclesSignal.set(value ?? []);
  }
  get vehicles(): FleetVehicle[] {
    return this.vehiclesSignal();
  }

  @Input()
  set reservations(value: Reservation[]) {
    this.reservationsSignal.set(value ?? []);
  }
  get reservations(): Reservation[] {
    return this.reservationsSignal();
  }

  @Input()
  set selectedVehicleId(value: number | null) {
    this.selectedVehicleIdSignal.set(value ?? null);
  }
  get selectedVehicleId(): number | null {
    return this.selectedVehicleIdSignal();
  }

  @Input() loading = false;

  @Output() clearVehicleFilter = new EventEmitter<void>();
  @Output() vehicleSelect = new EventEmitter<FleetVehicle>();

  readonly rangeDays = signal<RangeDays>(14);
  readonly startDate = signal<Date>(this.startOfDay(new Date()));

  readonly visibleVehicles = computed(() => {
    const selectedId = this.selectedVehicleIdSignal();
    const list = this.vehiclesSignal();
    if (selectedId === null) return list;
    return list.filter((v) => v.id === selectedId);
  });

  readonly selectedVehicle = computed(() => {
    const selectedId = this.selectedVehicleIdSignal();
    if (selectedId === null) return null;
    return this.vehiclesSignal().find((v) => v.id === selectedId) ?? null;
  });

  readonly days = computed<CalendarDay[]>(() => {
    const start = this.startDate();
    const count = this.rangeDays();
    const todayIso = this.toIsoDate(new Date());
    const result: CalendarDay[] = [];

    for (let i = 0; i < count; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      const iso = this.toIsoDate(d);
      const dow = d.getDay();
      result.push({
        date: d,
        iso,
        dayName: d.toLocaleDateString('fr-FR', { weekday: 'short' }).replace('.', ''),
        dayNumber: d.getDate(),
        monthLabel: d.toLocaleDateString('fr-FR', { month: 'short' }),
        isWeekend: dow === 0 || dow === 6,
        isToday: iso === todayIso,
      });
    }
    return result;
  });

  readonly periodTitle = computed(() => {
    const list = this.days();
    if (!list.length) return '';
    const first = list[0].date;
    const last = list[list.length - 1].date;
    const fmt: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short', year: 'numeric' };
    return `${first.toLocaleDateString('fr-FR', fmt)} – ${last.toLocaleDateString('fr-FR', fmt)}`;
  });

  setRange(days: RangeDays): void {
    this.rangeDays.set(days);
  }

  shiftPeriod(direction: -1 | 1): void {
    const step = this.rangeDays() === 30 ? 14 : 7;
    const next = new Date(this.startDate());
    next.setDate(next.getDate() + direction * step);
    this.startDate.set(this.startOfDay(next));
  }

  goToToday(): void {
    this.startDate.set(this.startOfDay(new Date()));
  }

  getBarsForVehicle(vehicleId: number): GanttBar[] {
    const rangeStart = this.startDate().getTime();
    const totalDays = this.rangeDays();
    const msPerDay = 86_400_000;
    const rangeEnd = rangeStart + totalDays * msPerDay;

    return this.reservationsSignal()
      .filter((r) => {
        const resVehicleId = r.vehicleId ?? r.vehicle?.id;
        return resVehicleId === vehicleId && r.statut !== 'CANCELLED' && r.dateDebut && r.dateFin;
      })
      .map((r) => {
        const start = this.parseIsoDate(r.dateDebut!).getTime();
        const end = this.parseIsoDate(r.dateFin!).getTime() + msPerDay; // inclusive end day

        if (end <= rangeStart || start >= rangeEnd) {
          return null;
        }

        const clampedStart = Math.max(start, rangeStart);
        const clampedEnd = Math.min(end, rangeEnd);
        const leftPercent = ((clampedStart - rangeStart) / (totalDays * msPerDay)) * 100;
        const widthPercent = Math.max(
          ((clampedEnd - clampedStart) / (totalDays * msPerDay)) * 100,
          100 / totalDays,
        );

        const client = r.clientNom ? ` · ${r.clientNom}` : '';
        const tooltip = `${r.code}${client} (${r.dateDebut} → ${r.dateFin}) · ${r.prixTotal ?? 0} MAD [${r.statut}]`;

        return {
          reservation: r,
          leftPercent,
          widthPercent: Math.min(widthPercent, 100 - leftPercent),
          tooltip,
          statusClass: `bar-${String(r.statut || '').toLowerCase()}`,
          label: `${r.code}${client}`,
        };
      })
      .filter((bar): bar is GanttBar => bar !== null);
  }

  onClearVehicleFilter(): void {
    this.clearVehicleFilter.emit();
  }

  onSelectVehicle(vehicle: FleetVehicle): void {
    this.vehicleSelect.emit(vehicle);
  }

  private startOfDay(date: Date): Date {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private parseIsoDate(iso: string): Date {
    const clean = iso.slice(0, 10);
    const [year, month, day] = clean.split('-').map(Number);
    return new Date(year, (month || 1) - 1, day || 1, 0, 0, 0, 0);
  }

  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
