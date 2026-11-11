import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Reservation, ReservationStatus } from '../../models/reservation.model';

@Component({
  selector: 'app-reservation-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './reservation-table.component.html',
  styleUrl: './reservation-table.component.scss',
})
export class ReservationTableComponent {
  @Input() reservations: Reservation[] = [];

  @Output() confirm = new EventEmitter<Reservation>();
  @Output() complete = new EventEmitter<Reservation>();
  @Output() cancelReservation = new EventEmitter<Reservation>();
  @Output() viewContract = new EventEmitter<Reservation>();
  @Output() manageCaution = new EventEmitter<Reservation>();

  readonly displayedColumns: string[] = [
    'code',
    'client',
    'vehicule',
    'dates',
    'statut',
    'prix',
    'actions',
  ];

  getStatusClass(statut: ReservationStatus | string): string {
    switch (statut) {
      case 'PENDING':
        return 'badge-pending';
      case 'CONFIRMED':
        return 'badge-confirmed';
      case 'ACTIVE':
        return 'badge-active';
      case 'COMPLETED':
        return 'badge-completed';
      case 'CANCELLED':
        return 'badge-cancelled';
      default:
        return 'badge-default';
    }
  }

  getStatusLabel(statut: ReservationStatus | string): string {
    switch (statut) {
      case 'PENDING':
        return 'En attente';
      case 'CONFIRMED':
        return 'Confirmée';
      case 'ACTIVE':
        return 'En cours';
      case 'COMPLETED':
        return 'Terminée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return statut;
    }
  }

  getVehicleLabel(reservation: Reservation): string {
    if (reservation.vehicle) {
      return `${reservation.vehicle.marque} ${reservation.vehicle.modele}`;
    }
    return `Véhicule #${reservation.vehicleId ?? 'N/A'}`;
  }

  getClientLabel(reservation: Reservation): string {
    if (reservation.clientNom) {
      return reservation.clientNom;
    }
    if (reservation.userId) {
      return `Client #${reservation.userId}`;
    }
    return 'Client';
  }
}
