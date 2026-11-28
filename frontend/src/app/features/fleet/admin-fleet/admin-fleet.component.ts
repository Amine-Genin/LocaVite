import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { catchError, of } from 'rxjs';

import {
  FleetFilter,
  FleetService,
  FleetStatus,
  FleetVehicle,
} from '../../vehicles/services/fleet.service';
import { Reservation } from '../../reservations/models/reservation.model';
import { ReservationService } from '../../reservations/services/reservation.service';
import { FleetTableComponent, StatusChangeEvent } from '../fleet-table/fleet-table.component';
import {
  FLEET_STATUS_LABELS,
  VehicleHistoryModalComponent,
} from '../vehicle-history-modal/vehicle-history-modal.component';
import { FleetCalendarComponent } from '../fleet-calendar/fleet-calendar.component';

export type FleetViewMode = 'table' | 'grid' | 'calendar';

export interface FleetStats {
  total: number;
  available: number;
  rented: number;
  maintenance: number;
}

@Component({
  selector: 'app-admin-fleet',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatMenuModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    FleetTableComponent,
    FleetCalendarComponent,
  ],
  templateUrl: './admin-fleet.component.html',
  styleUrl: './admin-fleet.component.scss',
})
export class AdminFleetComponent implements OnInit {
  private readonly fleetService = inject(FleetService);
  private readonly reservationService = inject(ReservationService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly vehicles = signal<FleetVehicle[]>([]);
  readonly allVehiclesSnapshot = signal<FleetVehicle[]>([]);
  readonly reservations = signal<Reservation[]>([]);
  readonly loading = signal<boolean>(true);
  readonly calendarLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly updatingVehicleId = signal<number | null>(null);

  readonly viewMode = signal<FleetViewMode>('table');
  readonly selectedCalendarVehicleId = signal<number | null>(null);

  // Filters & quick search
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<string>('');
  readonly categoryFilter = signal<string>('');
  readonly cityFilter = signal<string>('');

  readonly statusOptions: { value: FleetStatus; label: string }[] = [
    { value: 'PUBLISHED', label: 'Disponible' },
    { value: 'RENTED', label: 'En location' },
    { value: 'MAINTENANCE', label: 'En maintenance' },
    { value: 'ARCHIVED', label: 'Archivé' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'SOLD', label: 'Vendu' },
  ];

  readonly changeableStatuses: { value: FleetStatus; label: string }[] = [
    { value: 'PUBLISHED', label: 'Disponible' },
    { value: 'RENTED', label: 'En location' },
    { value: 'MAINTENANCE', label: 'En maintenance' },
    { value: 'ARCHIVED', label: 'Archivé' },
  ];

  readonly categories = computed<string[]>(() => {
    const source = this.allVehiclesSnapshot().length ? this.allVehiclesSnapshot() : this.vehicles();
    return [...new Set(source.map((v) => v.categorie).filter(Boolean))].sort((a, b) =>
      a.localeCompare(b),
    );
  });

  readonly cities = computed<string[]>(() => {
    const source = this.allVehiclesSnapshot().length ? this.allVehiclesSnapshot() : this.vehicles();
    return [...new Set(source.map((v) => v.ville).filter(Boolean))].sort((a, b) =>
      a.localeCompare(b),
    );
  });

  readonly filteredVehicles = computed<FleetVehicle[]>(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const list = this.vehicles();
    if (!query) return list;

    return list.filter((v) => {
      const haystack = [
        v.marque,
        v.modele,
        v.matricule,
        v.categorie,
        v.ville,
        v.localisationActuelle,
        String(v.annee),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(query);
    });
  });

  readonly stats = computed<FleetStats>(() => {
    const list = this.filteredVehicles();
    return {
      total: list.length,
      available: list.filter((v) => v.statut === 'PUBLISHED').length,
      rented: list.filter((v) => v.statut === 'RENTED').length,
      maintenance: list.filter((v) => v.statut === 'MAINTENANCE').length,
    };
  });

  readonly hasActiveFilters = computed(
    () =>
      Boolean(
        this.searchQuery().trim() ||
          this.statusFilter() ||
          this.categoryFilter() ||
          this.cityFilter(),
      ),
  );

  ngOnInit(): void {
    this.loadFleet();
  }

  loadFleet(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const filter: FleetFilter = {
      statut: this.statusFilter() || undefined,
      categorie: this.categoryFilter() || undefined,
      ville: this.cityFilter() || undefined,
    };

    const isUnfiltered = !filter.statut && !filter.categorie && !filter.ville;

    this.fleetService.getAll(filter).subscribe({
      next: (items) => {
        const list = items ?? [];
        this.vehicles.set(list);
        if (isUnfiltered || this.allVehiclesSnapshot().length === 0) {
          this.allVehiclesSnapshot.set(list);
        }
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les véhicules de la flotte.');
        this.loading.set(false);
        this.snackBar.open('Impossible de charger la flotte.', 'Fermer', { duration: 3500 });
      },
    });
  }

  loadReservationsForCalendar(): void {
    this.calendarLoading.set(true);
    this.reservationService
      .getAll({ size: 200, sort: 'dateDebut,asc' })
      .pipe(catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size: 200, number: 0 })))
      .subscribe((page) => {
        this.reservations.set(page?.content ?? []);
        this.calendarLoading.set(false);
      });
  }

  setViewMode(mode: FleetViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'calendar') {
      this.loadReservationsForCalendar();
    } else {
      this.selectedCalendarVehicleId.set(null);
    }
  }

  onFilterChange(): void {
    this.loadFleet();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('');
    this.categoryFilter.set('');
    this.cityFilter.set('');
    this.loadFleet();
  }

  onStatusChange(event: StatusChangeEvent): void {
    const { vehicle, statut } = event;
    if (vehicle.statut === statut) return;

    this.updatingVehicleId.set(vehicle.id);
    this.fleetService.updateStatus(vehicle.id, statut).subscribe({
      next: () => {
        this.updatingVehicleId.set(null);
        this.snackBar.open(
          `Statut de ${vehicle.marque} ${vehicle.modele} mis à jour (${this.getStatusLabel(statut)}).`,
          'OK',
          { duration: 3000 },
        );
        this.loadFleet();
      },
      error: () => {
        this.updatingVehicleId.set(null);
        this.snackBar.open('Erreur lors de la mise à jour du statut.', 'Fermer', {
          duration: 3500,
        });
      },
    });
  }

  onViewDetails(vehicle: FleetVehicle): void {
    this.router.navigate(['/vehicles', vehicle.id]);
  }

  onEditVehicle(vehicle: FleetVehicle): void {
    this.router.navigate(['/admin/vehicles/new'], {
      queryParams: { edit: vehicle.id },
    });
  }

  onViewHistory(vehicle: FleetVehicle): void {
    this.dialog.open(VehicleHistoryModalComponent, {
      data: { vehicle },
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'fleet-history-dialog',
    });
  }

  onViewCalendar(vehicle: FleetVehicle): void {
    this.selectedCalendarVehicleId.set(vehicle.id);
    this.viewMode.set('calendar');
    this.loadReservationsForCalendar();
  }

  onClearCalendarVehicle(): void {
    this.selectedCalendarVehicleId.set(null);
  }

  getStatusLabel(statut: FleetStatus | string): string {
    return FLEET_STATUS_LABELS[statut] ?? statut;
  }

  exportCsv(): void {
    const list = this.filteredVehicles();
    if (!list.length) {
      this.snackBar.open('Aucun véhicule à exporter.', 'OK', { duration: 2500 });
      return;
    }

    const headers = [
      'ID',
      'Immatriculation',
      'Marque',
      'Modèle',
      'Année',
      'Catégorie',
      'Statut',
      'Ville',
      'Localisation',
      'Prochaine réservation',
      'Revenus mois (MAD)',
      'Revenus année (MAD)',
      'Taux occupation (%)',
    ];

    const rows = list.map((v) => [
      v.id,
      v.matricule,
      v.marque,
      v.modele,
      v.annee,
      v.categorie,
      this.getStatusLabel(v.statut),
      v.ville,
      v.localisationActuelle || v.ville,
      v.prochaineReservation ?? '',
      v.revenusMois,
      v.revenusAnnee,
      v.tauxOccupation,
    ]);

    const csv =
      '\uFEFF' +
      [headers, ...rows]
        .map((row) => row.map((val) => `"${String(val ?? '').replaceAll('"', '""')}"`).join(';'))
        .join('\r\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `flotte-locavite-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }
}
