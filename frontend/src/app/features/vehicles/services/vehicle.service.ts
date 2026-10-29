import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, map } from 'rxjs';

import {
  CatalogEntry,
  PageMetadata,
  Vehicle,
  VehicleFilter,
  VehiclePageResponse,
  VehiclePhoto,
} from '../models/vehicle.model';

const API_URL = '/api/vehicles';
export const FAVORITES_STORAGE_KEY = 'locavite_favorite_vehicles';

interface RawVehicleApi extends Vehicle {
  primaryPhotoUrl?: string | null;
}

interface RawVehiclePageApi {
  content?: RawVehicleApi[];
  page?: Partial<PageMetadata>;
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private readonly http = inject(HttpClient);

  private readonly filterSubject = new BehaviorSubject<VehicleFilter>({
    page: 0,
    size: 12,
  });
  readonly filters$: Observable<VehicleFilter> = this.filterSubject.asObservable();

  private readonly favoritesSubject = new BehaviorSubject<Set<number>>(this.loadFavorites());
  readonly favorites$: Observable<Set<number>> = this.favoritesSubject.asObservable();

  get currentFilters(): VehicleFilter {
    return this.filterSubject.value;
  }

  updateFilters(partial: Partial<VehicleFilter>): void {
    this.filterSubject.next({
      ...this.filterSubject.value,
      ...partial,
    });
  }

  resetFilters(size = 12): void {
    this.filterSubject.next({ page: 0, size });
  }

  getBrands(): Observable<CatalogEntry[]> {
    return this.http.get<CatalogEntry[]>('/api/brands');
  }

  getCategories(): Observable<CatalogEntry[]> {
    return this.http.get<CatalogEntry[]>('/api/categories');
  }

  search(filter: VehicleFilter = {}): Observable<VehiclePageResponse> {
    let params = new HttpParams();

    const entries = Object.entries(filter) as [keyof VehicleFilter, unknown][];
    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }

    return this.http.get<RawVehiclePageApi>(API_URL, { params }).pipe(
      map((res) => {
        const content = (res?.content ?? []).map((item) => this.normalizeVehicle(item));
        const size = res?.page?.size ?? res?.size ?? filter.size ?? 12;
        const number = res?.page?.number ?? res?.number ?? filter.page ?? 0;
        const totalElements = res?.page?.totalElements ?? res?.totalElements ?? content.length;
        const totalPages =
          res?.page?.totalPages ??
          res?.totalPages ??
          (size > 0 ? Math.ceil(totalElements / size) : 0);

        return {
          content,
          page: {
            size,
            number,
            totalElements,
            totalPages,
          },
          totalElements,
          totalPages,
          size,
          number,
        };
      }),
    );
  }

  getById(id: number): Observable<Vehicle> {
    return this.http
      .get<RawVehicleApi>(`${API_URL}/${id}`)
      .pipe(map((item) => this.normalizeVehicle(item)));
  }

  isFavorite(vehicleId: number): boolean {
    return this.favoritesSubject.value.has(vehicleId);
  }

  toggleFavorite(vehicleId: number): boolean {
    const next = new Set(this.favoritesSubject.value);
    let added = false;
    if (next.has(vehicleId)) {
      next.delete(vehicleId);
    } else {
      next.add(vehicleId);
      added = true;
    }
    this.favoritesSubject.next(next);
    this.saveFavorites(next);
    return added;
  }

  private normalizeVehicle(item: RawVehicleApi): Vehicle {
    const primaryPhoto: VehiclePhoto | undefined =
      item.photos?.find((p) => p.isPrimary) ?? item.photos?.[0];
    const resolvedPhotoUrl =
      item.photoUrl ??
      item.primaryPhotoUrl ??
      primaryPhoto?.thumbnailUrl ??
      primaryPhoto?.url ??
      null;

    return {
      ...item,
      photoUrl: resolvedPhotoUrl,
      isFavorite: item.isFavorite ?? this.isFavorite(item.id),
    };
  }

  private loadFavorites(): Set<number> {
    try {
      const raw = localStorage.getItem(FAVORITES_STORAGE_KEY);
      if (!raw) return new Set<number>();
      const parsed: unknown = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return new Set(parsed.filter((v): v is number => typeof v === 'number'));
      }
    } catch {
      // Ignore storage errors
    }
    return new Set<number>();
  }

  private saveFavorites(ids: Set<number>): void {
    try {
      localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify([...ids]));
    } catch {
      // Ignore storage errors
    }
  }
}
