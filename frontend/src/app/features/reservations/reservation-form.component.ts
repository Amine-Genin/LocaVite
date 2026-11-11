import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs';
import { MatStepperModule } from '@angular/material/stepper';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { Vehicle } from '../vehicles/models/vehicle.model';
import { VehicleService } from '../vehicles/services/vehicle.service';
import { UserService } from '../profile/services/user.service';
import { User } from '../profile/models/user.model';
import { ReservationService } from './services/reservation.service';
import {
  PriceQuote,
  ReservationOption,
  ReservationRequest,
} from './models/reservation.model';

@Component({
  selector: 'app-reservation-form',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatStepperModule, MatDatepickerModule,
    MatNativeDateModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatCheckboxModule, MatRadioModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './reservation-form.component.html',
  styleUrl: './reservation-form.component.scss',
})
export class ReservationFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly vehicleService = inject(VehicleService);
  private readonly reservationService = inject(ReservationService);
  private readonly userService = inject(UserService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly today = new Date();
  readonly vehicle = signal<Vehicle | null>(null);
  readonly user = signal<User | null>(null);
  readonly quote = signal<PriceQuote | null>(null);
  readonly loading = signal(true);
  readonly checking = signal(false);
  readonly available = signal<boolean | null>(null);
  readonly submitting = signal(false);
  readonly documentsValid = signal(true);
  readonly error = signal('');

  readonly datesForm = this.fb.group({
    dateDebut: [null as Date | null, Validators.required],
    dateFin: [null as Date | null, Validators.required],
  });
  readonly placesForm = this.fb.nonNullable.group({
    lieuPriseEnCharge: ['', Validators.required],
    lieuRetour: ['', Validators.required],
  });
  readonly optionsForm = this.fb.nonNullable.group({
    gps: false,
    babySeat: false,
    extraDriver: false,
  });
  readonly confirmationForm = this.fb.nonNullable.group({
    acceptedTerms: [false, Validators.requiredTrue],
  });

  readonly locationOptions = ['Aéroport', 'Gare', 'Hôtel', 'Agence'];

  ngOnInit(): void {
    const vehicleId = Number(this.route.snapshot.paramMap.get('vehicleId'));
    if (!Number.isInteger(vehicleId) || vehicleId <= 0) {
      this.error.set('Le véhicule demandé est invalide.');
      this.loading.set(false);
      return;
    }

    this.vehicleService.getById(vehicleId).subscribe({
      next: (vehicle) => {
        this.vehicle.set(vehicle);
        this.loading.set(false);
        this.loadDocuments();
      },
      error: () => {
        this.error.set('Impossible de charger le véhicule.');
        this.loading.set(false);
      },
    });

    this.datesForm.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged((a, b) => a.dateDebut?.getTime() === b.dateDebut?.getTime()
        && a.dateFin?.getTime() === b.dateFin?.getTime()),
      filter(() => this.datesForm.valid),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.refreshQuoteAndAvailability());

    this.optionsForm.valueChanges.pipe(
      debounceTime(500),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      if (this.datesForm.valid) this.refreshQuote();
    });
  }

  refreshQuoteAndAvailability(): void {
    this.refreshQuote();
    const vehicleId = this.vehicle()?.id;
    const dates = this.dateValues();
    if (!vehicleId || !dates) return;
    this.checking.set(true);
    this.reservationService.checkAvailability(vehicleId, dates.from, dates.to).subscribe({
      next: (result) => {
        this.available.set(result.unavailable.length === 0);
        this.checking.set(false);
      },
      error: () => {
        this.available.set(null);
        this.checking.set(false);
        this.snackBar.open('Vérification de disponibilité impossible.', 'OK', { duration: 4000 });
      },
    });
  }

  submit(): void {
    if (this.confirmationForm.invalid || this.placesForm.invalid || this.datesForm.invalid
      || !this.vehicle() || this.available() !== true || !this.documentsValid()) {
      this.confirmationForm.markAllAsTouched();
      return;
    }
    const dates = this.dateValues();
    if (!dates) return;
    const request: ReservationRequest = {
      vehicleId: this.vehicle()!.id,
      dateDebut: dates.from,
      dateFin: dates.to,
      ...this.placesForm.getRawValue(),
      options: this.selectedOptions(),
    };
    this.submitting.set(true);
    this.reservationService.create(request).subscribe({
      next: (reservation) => {
        this.submitting.set(false);
        void this.router.navigate(['/reservations', reservation.id]);
      },
      error: (err: { error?: { detail?: string; message?: string } }) => {
        this.submitting.set(false);
        this.snackBar.open(
          err.error?.detail ?? err.error?.message ?? 'La réservation n’a pas pu être créée.',
          'OK',
          { duration: 5000 },
        );
      },
    });
  }

  selectedOptions(): ReservationOption[] {
    const value = this.optionsForm.getRawValue();
    return [
      value.gps ? 'GPS' : null,
      value.babySeat ? 'BABY_SEAT' : null,
      value.extraDriver ? 'EXTRA_DRIVER' : null,
    ].filter((option): option is ReservationOption => option !== null);
  }

  formatPrice(value: number | undefined): string {
    return `${(value ?? 0).toFixed(2)} MAD`;
  }

  private refreshQuote(): void {
    const vehicleId = this.vehicle()?.id;
    const dates = this.dateValues();
    if (!vehicleId || !dates) return;
    this.reservationService.getQuote({
      vehicleId, dateDebut: dates.from, dateFin: dates.to, options: this.selectedOptions(),
    }).subscribe({
      next: (quote) => this.quote.set(quote),
      error: () => this.quote.set(null),
    });
  }

  private dateValues(): { from: string; to: string } | null {
    const { dateDebut, dateFin } = this.datesForm.getRawValue();
    if (!dateDebut || !dateFin || dateFin <= dateDebut) return null;
    return { from: this.toIsoDate(dateDebut), to: this.toIsoDate(dateFin) };
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private loadDocuments(): void {
    this.userService.getProfile().subscribe({
      next: (user) => {
        this.user.set(user);
        const valid = ['permis', 'cin'].every((type) =>
          user.documents.some((document) => document.type === type && document.status === 'validated'),
        );
        this.documentsValid.set(valid);
      },
      error: () => this.documentsValid.set(false),
    });
  }
}
