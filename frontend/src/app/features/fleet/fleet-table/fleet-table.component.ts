import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';

import { FleetStatus, FleetVehicle } from '../../vehicles/services/fleet.service';
import { FLEET_STATUS_LABELS } from '../vehicle-history-modal/vehicle-history-modal.component';

export interface StatusChangeEvent {
  vehicle: FleetVehicle;
  statut: FleetStatus;
}

@Component({
  selector: 'app-fleet-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatMenuModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  templateUrl: './fleet-table.component.html',
  styleUrl: './fleet-table.component.scss',
})
export class FleetTableComponent implements OnChanges, AfterViewInit {
  @Input() vehicles: FleetVehicle[] = [];
  @Input() updatingVehicleId: number | null = null;

  @Output() viewDetails = new EventEmitter<FleetVehicle>();
  @Output() editVehicle = new EventEmitter<FleetVehicle>();
  @Output() statusChange = new EventEmitter<StatusChangeEvent>();
  @Output() viewHistory = new EventEmitter<FleetVehicle>();
  @Output() viewCalendar = new EventEmitter<FleetVehicle>();

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  readonly displayedColumns: string[] = [
    'photo',
    'vehicle',
    'statut',
    'localisation',
    'prochaineReservation',
    'revenus',
    'tauxOccupation',
    'actions',
  ];

  readonly availableStatuses: { value: FleetStatus; label: string }[] = [
    { value: 'PUBLISHED', label: 'Disponible' },
    { value: 'RENTED', label: 'En location' },
    { value: 'MAINTENANCE', label: 'En maintenance' },
    { value: 'ARCHIVED', label: 'Archivé' },
  ];

  readonly dataSource = new MatTableDataSource<FleetVehicle>([]);

  constructor() {
    this.dataSource.sortingDataAccessor = (item: FleetVehicle, property: string): string | number => {
      switch (property) {
        case 'vehicle':
          return `${item.marque} ${item.modele}`.toLowerCase();
        case 'statut':
          return item.statut;
        case 'localisation':
          return (item.localisationActuelle || item.ville || '').toLowerCase();
        case 'prochaineReservation':
          return item.prochaineReservation ? new Date(item.prochaineReservation).getTime() : Number.MAX_SAFE_INTEGER;
        case 'revenus':
          return Number(item.revenusMois) || 0;
        case 'tauxOccupation':
          return Number(item.tauxOccupation) || 0;
        default:
          return item.id;
      }
    };
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['vehicles']) {
      this.dataSource.data = this.vehicles ?? [];
      if (this.paginator) {
        this.paginator.firstPage();
      }
    }
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  getStatusLabel(statut: FleetStatus | string): string {
    return FLEET_STATUS_LABELS[statut] ?? statut;
  }

  onChangeStatus(vehicle: FleetVehicle, statut: FleetStatus): void {
    if (vehicle.statut === statut) return;
    this.statusChange.emit({ vehicle, statut });
  }

  getOccupancyClass(rate: number): string {
    if (rate >= 70) return 'occ-high';
    if (rate >= 35) return 'occ-mid';
    return 'occ-low';
  }
}
