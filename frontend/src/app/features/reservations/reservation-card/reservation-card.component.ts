import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Reservation, ReservationStatus } from '../models/reservation.model';

@Component({
  selector: 'app-reservation-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './reservation-card.component.html',
  styleUrl: './reservation-card.component.scss',
})
export class ReservationCardComponent {
  @Input({ required: true }) reservation!: Reservation;

  @Output() viewDetails = new EventEmitter<Reservation>();
  @Output() downloadContract = new EventEmitter<Reservation>();
  @Output() extend = new EventEmitter<Reservation>();
  @Output() cancelReservation = new EventEmitter<Reservation>();
  @Output() contactAgency = new EventEmitter<Reservation>();

  get status(): ReservationStatus | string {
    return this.reservation.statut;
  }

  get statusClass(): string {
    switch (this.reservation.statut) {
      case 'PENDING':
        return 'status-pending';
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'ACTIVE':
        return 'status-active';
      case 'COMPLETED':
        return 'status-completed';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-default';
    }
  }

  get statusLabel(): string {
    switch (this.reservation.statut) {
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
        return this.reservation.statut;
    }
  }

  get canCancel(): boolean {
    return this.reservation.statut === 'PENDING' || this.reservation.statut === 'CONFIRMED';
  }

  get canExtend(): boolean {
    return this.reservation.statut === 'CONFIRMED' || this.reservation.statut === 'ACTIVE';
  }

  get canDownloadContract(): boolean {
    return this.reservation.statut === 'CONFIRMED' || this.reservation.statut === 'ACTIVE' || this.reservation.statut === 'COMPLETED';
  }

  get vehicleTitle(): string {
    if (this.reservation.vehicle) {
      return `${this.reservation.vehicle.marque} ${this.reservation.vehicle.modele}`;
    }
    return `Véhicule #${this.reservation.vehicleId ?? ''}`;
  }

  get vehiclePhotoUrl(): string {
    if (this.reservation.vehicle?.photoUrl) {
      return this.reservation.vehicle.photoUrl;
    }
    return 'assets/images/placeholder-car.jpg';
  }

  onViewDetails(): void {
    this.viewDetails.emit(this.reservation);
  }

  onDownloadContract(): void {
    this.downloadContract.emit(this.reservation);
  }

  onExtend(): void {
    this.extend.emit(this.reservation);
  }

  onCancel(): void {
    this.cancelReservation.emit(this.reservation);
  }

  onContactAgency(): void {
    this.contactAgency.emit(this.reservation);
  }
}
