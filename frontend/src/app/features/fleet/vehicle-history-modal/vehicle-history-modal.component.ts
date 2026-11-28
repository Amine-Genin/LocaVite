import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import {
  FleetHistory,
  FleetService,
  FleetStatus,
  FleetVehicle,
} from '../../vehicles/services/fleet.service';

export interface VehicleHistoryModalData {
  vehicle: FleetVehicle;
}

export const FLEET_STATUS_LABELS: Record<FleetStatus | string, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Disponible',
  RENTED: 'En location',
  MAINTENANCE: 'En maintenance',
  SOLD: 'Vendu',
  ARCHIVED: 'Archivé',
};

@Component({
  selector: 'app-vehicle-history-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './vehicle-history-modal.component.html',
  styleUrl: './vehicle-history-modal.component.scss',
})
export class VehicleHistoryModalComponent implements OnInit {
  private readonly fleetService = inject(FleetService);
  private readonly dialogRef = inject(MatDialogRef<VehicleHistoryModalComponent>);
  readonly data = inject<VehicleHistoryModalData>(MAT_DIALOG_DATA);

  readonly events = signal<FleetHistory[]>([]);
  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly activeTypeFilter = signal<string>('ALL');

  readonly eventTypes = ['ALL', 'STATUT', 'RESERVATION', 'MAINTENANCE', 'INCIDENT', 'TRANSFERT'];

  readonly filteredEvents = computed(() => {
    const filter = this.activeTypeFilter();
    const list = this.events();
    if (filter === 'ALL') return list;
    return list.filter((event) => (event.type || '').toUpperCase() === filter);
  });

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.loading.set(true);
    this.error.set(null);

    this.fleetService.getHistory(this.data.vehicle.id).subscribe({
      next: (items) => {
        this.events.set(items ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'historique du véhicule.");
        this.loading.set(false);
      },
    });
  }

  setTypeFilter(type: string): void {
    this.activeTypeFilter.set(type);
  }

  getTypeLabel(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'ALL':
        return 'Tous';
      case 'STATUT':
        return 'Statut';
      case 'RESERVATION':
        return 'Réservation';
      case 'MAINTENANCE':
        return 'Maintenance';
      case 'INCIDENT':
        return 'Incident';
      case 'TRANSFERT':
        return 'Transfert';
      default:
        return type;
    }
  }

  getStatusLabel(status?: string): string {
    if (!status) return '';
    return FLEET_STATUS_LABELS[status] ?? status;
  }

  getEventIcon(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'STATUT':
        return 'swap_horiz';
      case 'RESERVATION':
        return 'event_available';
      case 'MAINTENANCE':
        return 'build';
      case 'INCIDENT':
        return 'warning';
      case 'TRANSFERT':
        return 'sync_alt';
      default:
        return 'history';
    }
  }

  getEventClass(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'STATUT':
        return 'type-statut';
      case 'RESERVATION':
        return 'type-reservation';
      case 'MAINTENANCE':
        return 'type-maintenance';
      case 'INCIDENT':
        return 'type-incident';
      case 'TRANSFERT':
        return 'type-transfert';
      default:
        return 'type-default';
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
