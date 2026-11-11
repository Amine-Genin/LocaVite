import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Reservation, ReservationPageResponse } from './models/reservation.model';
import { ReservationService } from './services/reservation.service';
import { VehicleService } from '../vehicles/services/vehicle.service';
import { ReservationCardComponent } from './reservation-card/reservation-card.component';
import { VehicleContactDialogComponent } from '../vehicles/vehicle-detail/contact-dialog/vehicle-contact-dialog.component';

export type ReservationTab = 'upcoming' | 'active' | 'completed' | 'cancelled';

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTabsModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    ReservationCardComponent,
  ],
  templateUrl: './my-reservations.component.html',
  styleUrl: './my-reservations.component.scss',
})
export class MyReservationsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly vehicleService = inject(VehicleService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly allReservations = signal<Reservation[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Pagination
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);

  // Tab sélectionné
  readonly currentTab = signal<ReservationTab>('upcoming');

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getMyReservations(0, 100).subscribe({
      next: (response: ReservationPageResponse) => {
        const reservations = response?.content ?? [];
        this.fetchVehiclesForReservations(reservations);
      },
      error: (_err: unknown) => {
        this.isLoading.set(false);
        this.errorMessage.set('Impossible de charger vos réservations.');
      },
    });
  }

  private fetchVehiclesForReservations(reservations: Reservation[]): void {
    if (!reservations.length) {
      this.allReservations.set([]);
      this.isLoading.set(false);
      return;
    }

    const vehicleRequests = reservations.map((res) => {
      if (res.vehicle) {
        return of(res.vehicle);
      }
      if (res.vehicleId) {
        return this.vehicleService.getVehicleById(res.vehicleId).pipe(
          catchError(() => of(undefined)),
        );
      }
      return of(undefined);
    });

    forkJoin(vehicleRequests).subscribe({
      next: (vehicles) => {
        const enriched = reservations.map((res, index) => ({
          ...res,
          vehicle: vehicles[index] || res.vehicle,
        }));
        this.allReservations.set(enriched);
        this.isLoading.set(false);
      },
      error: () => {
        this.allReservations.set(reservations);
        this.isLoading.set(false);
      },
    });
  }

  // Filtrage selon l'onglet
  getFilteredReservations(tab: ReservationTab): Reservation[] {
    const list = this.allReservations();
    const today = new Date().toISOString().split('T')[0];

    let filtered: Reservation[] = [];

    switch (tab) {
      case 'upcoming':
        // Statut PENDING ou CONFIRMED (avec date de fin >= aujourd'hui)
        filtered = list.filter(
          (r) =>
            (r.statut === 'PENDING' || r.statut === 'CONFIRMED') &&
            (!r.dateFin || r.dateFin >= today),
        );
        break;
      case 'active':
        filtered = list.filter((r) => r.statut === 'ACTIVE');
        break;
      case 'completed':
        // COMPLETED ou réservations passées
        filtered = list.filter(
          (r) =>
            r.statut === 'COMPLETED' ||
            (r.statut !== 'CANCELLED' && r.dateFin && r.dateFin < today && r.statut !== 'ACTIVE'),
        );
        break;
      case 'cancelled':
        filtered = list.filter((r) => r.statut === 'CANCELLED');
        break;
    }

    // Tri par date (les plus récentes / prochaines d'abord)
    return filtered.sort((a, b) => {
      const dateA = a.dateDebut || a.createdAt || '';
      const dateB = b.dateDebut || b.createdAt || '';
      return dateB.localeCompare(dateA);
    });
  }

  get displayedReservations(): Reservation[] {
    const filtered = this.getFilteredReservations(this.currentTab());
    const start = this.pageIndex() * this.pageSize();
    return filtered.slice(start, start + this.pageSize());
  }

  get currentTabCount(): number {
    return this.getFilteredReservations(this.currentTab()).length;
  }

  onTabChange(index: number): void {
    const tabs: ReservationTab[] = ['upcoming', 'active', 'completed', 'cancelled'];
    this.currentTab.set(tabs[index] || 'upcoming');
    this.pageIndex.set(0);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  // Actions
  onViewDetails(reservation: Reservation): void {
    this.router.navigate(['/reservations', reservation.id]);
  }

  onDownloadContract(reservation: Reservation): void {
    this.reservationService.downloadContract(reservation.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrat-reservation-${reservation.code || reservation.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open('Erreur lors du téléchargement du contrat.', 'Fermer', {
          duration: 4000,
        });
      },
    });
  }

  onExtend(reservation: Reservation): void {
    // Navigation ou ouverture dialogue de prolongation
    this.router.navigate(['/reservations', reservation.id], {
      queryParams: { action: 'extend' },
    });
  }

  onCancel(reservation: Reservation): void {
    const confirmed = window.confirm(
      `Êtes-vous sûr de vouloir annuler la réservation ${reservation.code} ?`,
    );
    if (!confirmed) return;

    this.isLoading.set(true);
    this.reservationService.cancel(reservation.id).subscribe({
      next: () => {
        this.snackBar.open('Réservation annulée avec succès.', 'OK', { duration: 3000 });
        this.loadReservations();
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open("Échec de l'annulation de la réservation.", 'Fermer', {
          duration: 4000,
        });
      },
    });
  }

  onContactAgency(reservation: Reservation): void {
    if (reservation.vehicle) {
      this.dialog.open(VehicleContactDialogComponent, {
        data: { vehicle: reservation.vehicle },
        width: '500px',
      });
    } else if (reservation.vehicleId) {
      this.vehicleService.getVehicleById(reservation.vehicleId).subscribe({
        next: (vehicle) => {
          this.dialog.open(VehicleContactDialogComponent, {
            data: { vehicle },
            width: '500px',
          });
        },
        error: () => {
          this.snackBar.open("Impossible de charger les coordonnées de l'agence.", 'Fermer', {
            duration: 3000,
          });
        },
      });
    } else {
      this.snackBar.open('Informations de contact non disponibles.', 'Fermer', {
        duration: 3000,
      });
    }
  }
}
