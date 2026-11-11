import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  Reservation,
  ReservationFilterParams,
  ReservationPageResponse,
} from '../models/reservation.model';
import { ReservationService } from '../services/reservation.service';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { StatsCardsComponent, ReservationStats } from './stats-cards/stats-cards.component';
import { ReservationTableComponent } from './reservation-table/reservation-table.component';

@Component({
  selector: 'app-admin-reservations',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    StatsCardsComponent,
    ReservationTableComponent,
  ],
  templateUrl: './admin-reservations.component.html',
  styleUrl: './admin-reservations.component.scss',
})
export class AdminReservationsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly vehicleService = inject(VehicleService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly filterForm: FormGroup = this.fb.group({
    code: [''],
    statut: [''],
    dateDebut: [''],
    dateFin: [''],
  });

  readonly reservations = signal<Reservation[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Pagination
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);

  // Statistiques
  readonly stats = signal<ReservationStats>({
    totalCount: 0,
    monthlyRevenue: 0,
    occupancyRate: 0,
    activeCount: 0,
    pendingCount: 0,
  });

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const formValues = this.filterForm.value;
    const filterParams: ReservationFilterParams = {
      page: this.pageIndex(),
      size: this.pageSize(),
      code: formValues.code?.trim() || undefined,
      statut: formValues.statut || undefined,
      dateDebut: formValues.dateDebut || undefined,
      dateFin: formValues.dateFin || undefined,
      sort: 'createdAt,desc',
    };

    this.reservationService.getAll(filterParams).subscribe({
      next: (response: ReservationPageResponse) => {
        const items = response?.content ?? [];
        this.totalElements.set(response?.totalElements ?? items.length);
        this.fetchVehiclesAndCalculateStats(items);
      },
      error: (_err: unknown) => {
        this.isLoading.set(false);
        this.errorMessage.set('Erreur lors du chargement des réservations.');
      },
    });
  }

  private fetchVehiclesAndCalculateStats(items: Reservation[]): void {
    if (!items.length) {
      this.reservations.set([]);
      this.calculateStats([]);
      this.isLoading.set(false);
      return;
    }

    const vehicleRequests = items.map((res) => {
      if (res.vehicle) return of(res.vehicle);
      if (res.vehicleId) {
        return this.vehicleService.getVehicleById(res.vehicleId).pipe(
          catchError(() => of(undefined)),
        );
      }
      return of(undefined);
    });

    forkJoin(vehicleRequests).subscribe({
      next: (vehicles) => {
        const enriched = items.map((res, i) => ({
          ...res,
          vehicle: vehicles[i] || res.vehicle,
        }));
        this.reservations.set(enriched);
        this.calculateStats(enriched);
        this.isLoading.set(false);
      },
      error: () => {
        this.reservations.set(items);
        this.calculateStats(items);
        this.isLoading.set(false);
      },
    });
  }

  private calculateStats(items: Reservation[]): void {
    const totalCount = this.totalElements();
    const pendingCount = items.filter((r) => r.statut === 'PENDING').length;
    const activeCount = items.filter((r) => r.statut === 'ACTIVE').length;

    // Revenus cumulés des réservations non annulées
    const monthlyRevenue = items
      .filter((r) => r.statut !== 'CANCELLED')
      .reduce((acc, curr) => acc + (Number(curr.prixTotal) || 0), 0);

    // Taux d'occupation estimé
    const occupancyRate = items.length ? Math.min(100, Math.round((activeCount / items.length) * 100)) : 0;

    this.stats.set({
      totalCount,
      monthlyRevenue,
      occupancyRate,
      activeCount,
      pendingCount,
    });
  }

  onFilterSubmit(): void {
    this.pageIndex.set(0);
    this.loadReservations();
  }

  onResetFilters(): void {
    this.filterForm.reset({
      code: '',
      statut: '',
      dateDebut: '',
      dateFin: '',
    });
    this.pageIndex.set(0);
    this.loadReservations();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadReservations();
  }

  // Actions d'administration
  onConfirm(reservation: Reservation): void {
    this.reservationService.confirm(reservation.id).subscribe({
      next: () => {
        this.snackBar.open(`Réservation ${reservation.code} confirmée avec succès.`, 'OK', {
          duration: 3000,
        });
        this.loadReservations();
      },
      error: () => {
        this.snackBar.open('Erreur lors de la confirmation.', 'Fermer', { duration: 4000 });
      },
    });
  }

  onComplete(reservation: Reservation): void {
    this.reservationService.complete(reservation.id).subscribe({
      next: () => {
        this.snackBar.open(`Réservation ${reservation.code} clôturée.`, 'OK', { duration: 3000 });
        this.loadReservations();
      },
      error: () => {
        this.snackBar.open('Erreur lors de la clôture de la réservation.', 'Fermer', {
          duration: 4000,
        });
      },
    });
  }

  onCancel(reservation: Reservation): void {
    const reason = window.prompt(
      `Confirmer l'annulation de la réservation ${reservation.code}. Motif :`,
      'Demande client / Annulation agence',
    );
    if (reason === null) return;

    this.reservationService.cancel(reservation.id).subscribe({
      next: () => {
        this.snackBar.open(`Réservation ${reservation.code} annulée.`, 'OK', { duration: 3000 });
        this.loadReservations();
      },
      error: () => {
        this.snackBar.open("Échec de l'annulation.", 'Fermer', { duration: 4000 });
      },
    });
  }

  onViewContract(reservation: Reservation): void {
    this.reservationService.downloadContract(reservation.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => {
        this.snackBar.open('Contrat non disponible ou non généré.', 'Fermer', { duration: 3000 });
      },
    });
  }

  onManageCaution(reservation: Reservation): void {
    this.snackBar.open(
      `Caution: ${reservation.caution ?? 0} MAD - Statut: ${reservation.cautionStatut ?? 'PENDING'}`,
      'Fermer',
      { duration: 4000 },
    );
  }

  // Export CSV
  exportCsv(): void {
    const list = this.reservations();
    if (!list.length) {
      this.snackBar.open('Aucune réservation à exporter.', 'OK', { duration: 2500 });
      return;
    }

    const headers = ['Code', 'Client', 'Véhicule', 'Date Début', 'Date Fin', 'Statut', 'Prix Total (MAD)', 'Caution (MAD)'];
    const rows = list.map((r) => [
      r.code,
      r.clientNom || `ID ${r.userId || ''}`,
      r.vehicle ? `${r.vehicle.marque} ${r.vehicle.modele}` : `ID ${r.vehicleId || ''}`,
      r.dateDebut || '',
      r.dateFin || '',
      r.statut,
      r.prixTotal,
      r.caution || 0,
    ]);

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers.join(';'), ...rows.map((e) => e.join(';'))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `reservations-export-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
