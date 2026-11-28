import { Component, DestroyRef, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs/operators';
import { catchError, of } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Vehicle } from '../../vehicles/models/vehicle.model';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { ReservationOption } from '../../reservations/models/reservation.model';
import {
  OptionPrice,
  PricingAdminService,
  PricingSimulateRequest,
  PricingSimulateResponse,
} from '../services/pricing-admin.service';

@Component({
  selector: 'app-pricing-simulator',
  standalone: true,
  providers: [provideNativeDateAdapter()],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pricing-simulator.component.html',
  styleUrl: './pricing-simulator.component.scss',
})
export class PricingSimulatorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly pricingAdminService = inject(PricingAdminService);
  private readonly vehicleService = inject(VehicleService);
  private readonly destroyRef = inject(DestroyRef);

  @Input() availableOptions: OptionPrice[] = [];

  readonly vehicles = signal<Vehicle[]>([]);
  readonly result = signal<PricingSimulateResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly selectedOptions = signal<Set<ReservationOption>>(new Set());

  readonly defaultOptionChoices: { type: ReservationOption; libelle: string }[] = [
    { type: 'GPS', libelle: 'GPS Navigation' },
    { type: 'BABY_SEAT', libelle: 'Siège bébé' },
    { type: 'EXTRA_DRIVER', libelle: 'Conducteur additionnel' },
  ];

  readonly form = this.fb.group({
    vehicleId: [null as number | null],
    prixJour: [400, [Validators.min(1)]],
    dateDebut: [this.offsetDate(1), [Validators.required]],
    dateFin: [this.offsetDate(5), [Validators.required]],
    promoCode: [''],
  });

  get optionChoices(): { type: ReservationOption; libelle: string; prixJour?: number }[] {
    if (this.availableOptions?.length) {
      return this.availableOptions
        .filter((o) => o.actif)
        .map((o) => ({ type: o.type, libelle: o.libelle || o.type, prixJour: o.prixJour }));
    }
    return this.defaultOptionChoices;
  }

  ngOnInit(): void {
    this.vehicleService
      .search({ size: 100 })
      .pipe(
        catchError(() => of({ content: [] })),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        const list = page?.content ?? [];
        this.vehicles.set(list);
        if (list.length > 0 && !this.form.get('vehicleId')?.value) {
          this.form.patchValue({ vehicleId: list[0].id }, { emitEvent: false });
        }
        this.simulate();
      });

    this.form.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.simulate());
  }

  toggleOption(option: ReservationOption, checked: boolean): void {
    const next = new Set(this.selectedOptions());
    if (checked) {
      next.add(option);
    } else {
      next.delete(option);
    }
    this.selectedOptions.set(next);
    this.simulate();
  }

  isOptionSelected(option: ReservationOption): boolean {
    return this.selectedOptions().has(option);
  }

  simulate(): void {
    const v = this.form.getRawValue();
    const start = v.dateDebut;
    const end = v.dateFin;
    if (!start || !end || start.getTime() >= end.getTime()) {
      this.error.set('La date de début doit être antérieure à la date de fin.');
      this.result.set(null);
      return;
    }

    if (!v.vehicleId && (!v.prixJour || v.prixJour <= 0)) {
      this.error.set('Sélectionnez un véhicule ou saisissez un prix journalier.');
      this.result.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const payload: PricingSimulateRequest = {
      vehicleId: v.vehicleId ?? null,
      prixJour: v.vehicleId ? null : Number(v.prixJour),
      dateDebut: this.formatIsoDate(start),
      dateFin: this.formatIsoDate(end),
      options: [...this.selectedOptions()],
      promoCode: v.promoCode?.trim() || null,
    };

    this.pricingAdminService.simulatePrice(payload).subscribe({
      next: (quote) => {
        this.result.set(quote);
        this.loading.set(false);
      },
      error: (err) => {
        const detail = err?.error?.detail || 'Impossible de calculer le prix pour ces paramètres.';
        this.error.set(detail);
        this.result.set(null);
        this.loading.set(false);
      },
    });
  }

  private offsetDate(days: number): Date {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + days);
    return d;
  }

  private formatIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
