import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Vehicle, VehicleFilter } from '../models/vehicle.model';
import { VehicleService } from '../services/vehicle.service';
import { VehicleCardComponent } from '../vehicle-card/vehicle-card.component';
import { AuthHeaderComponent } from '../../../shared/components/auth-header/auth-header.component';
import { AuthFooterComponent } from '../../../shared/components/auth-footer/auth-footer.component';
import { setupFallbackTranslations } from '../vehicles.constants';

@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [
    MatCardModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    TranslateModule,
    VehicleCardComponent,
    AuthHeaderComponent,
    AuthFooterComponent,
  ],
  templateUrl: './vehicle-list.component.html',
  styleUrl: './vehicle-list.component.scss',
})
export class VehicleListComponent implements OnInit, OnDestroy {
  private readonly vehicleService = inject(VehicleService);
  private readonly translate = inject(TranslateService, { optional: true });

  readonly pageSizeOptions = [8, 12, 24];
  readonly skeletonItems = [1, 2, 3, 4, 5, 6];

  readonly filters$ = new BehaviorSubject<VehicleFilter>({
    page: 0,
    size: 12,
  });

  vehicles = signal<Vehicle[]>([]);
  totalElements = signal(0);
  pageIndex = signal(0);
  pageSize = signal(12);
  loading = signal(true);
  error = signal(false);

  private filterSub?: Subscription;
  private requestSub?: Subscription;

  constructor() {
    setupFallbackTranslations(this.translate);
  }

  ngOnInit(): void {
    this.filterSub = this.filters$.subscribe((filter) => {
      this.fetchVehicles(filter);
    });
  }

  ngOnDestroy(): void {
    this.filterSub?.unsubscribe();
    this.requestSub?.unsubscribe();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    const nextFilter: VehicleFilter = {
      ...this.filters$.value,
      page: event.pageIndex,
      size: event.pageSize,
    };
    this.vehicleService.updateFilters?.(nextFilter);
    this.filters$.next(nextFilter);
  }

  applyFilter(partial: Partial<VehicleFilter>): void {
    this.pageIndex.set(0);
    const nextFilter: VehicleFilter = {
      ...this.filters$.value,
      ...partial,
      page: 0,
      size: this.pageSize(),
    };
    this.vehicleService.updateFilters?.(nextFilter);
    this.filters$.next(nextFilter);
  }

  resetFilters(): void {
    this.pageIndex.set(0);
    const reset: VehicleFilter = {
      page: 0,
      size: this.pageSize(),
    };
    this.vehicleService.resetFilters?.(this.pageSize());
    this.filters$.next(reset);
  }

  onFavoriteToggle(vehicle: Vehicle): void {
    const isFav =
      typeof this.vehicleService.toggleFavorite === 'function'
        ? this.vehicleService.toggleFavorite(vehicle.id)
        : Boolean(vehicle.isFavorite);

    this.vehicles.update((list) =>
      list.map((v) => (v.id === vehicle.id ? { ...v, isFavorite: isFav } : v)),
    );
  }

  isVehicleFavorite(vehicle: Vehicle): boolean {
    if (typeof this.vehicleService.isFavorite === 'function') {
      return this.vehicleService.isFavorite(vehicle.id) || Boolean(vehicle.isFavorite);
    }
    return Boolean(vehicle.isFavorite);
  }

  private fetchVehicles(filter: VehicleFilter): void {
    this.loading.set(true);
    this.error.set(false);

    this.requestSub?.unsubscribe();
    this.requestSub = this.vehicleService.search(filter).subscribe({
      next: (res) => {
        const content = res?.content ?? [];
        const total = res?.page?.totalElements ?? res?.totalElements ?? content.length;
        const currentNumber = res?.page?.number ?? res?.number ?? filter.page ?? 0;
        const currentSize = res?.page?.size ?? res?.size ?? filter.size ?? 12;

        this.vehicles.set(content);
        this.totalElements.set(total);
        this.pageIndex.set(currentNumber);
        this.pageSize.set(currentSize);
        this.loading.set(false);
      },
      error: () => {
        this.vehicles.set([]);
        this.totalElements.set(0);
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }
}
