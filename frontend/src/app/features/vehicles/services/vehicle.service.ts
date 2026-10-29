import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, map } from 'rxjs';

import {
  AvailabilityResponse,
  CatalogEntry,
  PageMetadata,
  Vehicle,
  VehicleCreateRequest,
  VehicleFilter,
  VehiclePageResponse,
  VehiclePhoto,
} from '../models/vehicle.model';

const API_URL = '/api/vehicles';
const BRANDS_API_URL = '/api/brands';
const CATEGORIES_API_URL = '/api/categories';
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

@Injectable({
  providedIn: 'root',
})
export class VehicleService {
  private readonly http = inject(HttpClient);
  private readonly favoritesSubject = new BehaviorSubject<Set<number>>(this.loadFavorites());

  readonly favorites$ = this.favoritesSubject.asObservable();

  getVehicles(filter: VehicleFilter = {}): Observable<VehiclePageResponse> {
    let params = new HttpParams();

    if (filter.marque) params = params.set('marque', filter.marque);
    if (filter.categorie) params = params.set('categorie', filter.categorie);
    if (filter.ville) params = params.set('ville', filter.ville);
    if (filter.carburant) params = params.set('carburant', filter.carburant);
    if (filter.transmission) params = params.set('transmission', filter.transmission);
    if (filter.brandId !== undefined && filter.brandId !== null) {
      params = params.set('brandId', filter.brandId.toString());
    }
    if (filter.categoryId !== undefined && filter.categoryId !== null) {
      params = params.set('categoryId', filter.categoryId.toString());
    }
    if (filter.dateDebut) params = params.set('dateDebut', filter.dateDebut);
    if (filter.dateFin) params = params.set('dateFin', filter.dateFin);
    if (filter.prixMin !== undefined && filter.prixMin !== null) {
      params = params.set('prixMin', filter.prixMin.toString());
    }
    if (filter.prixMax !== undefined && filter.prixMax !== null) {
      params = params.set('prixMax', filter.prixMax.toString());
    }
    if (filter.page !== undefined && filter.page !== null) {
      params = params.set('page', filter.page.toString());
    }
    if (filter.size !== undefined && filter.size !== null) {
      params = params.set('size', filter.size.toString());
    }
    if (filter.sort) params = params.set('sort', filter.sort);

    return this.http.get<RawVehiclePageApi>(API_URL, { params }).pipe(
      map((response) => {
        const rawList = response.content ?? [];
        const content = rawList.map((item) => this.normalizeVehicle(item));
        const totalElements = response.totalElements ?? response.page?.totalElements ?? content.length;
        const totalPages = response.totalPages ?? response.page?.totalPages ?? 1;
        const size = response.size ?? response.page?.size ?? (content.length || 12);
        const number = response.number ?? response.page?.number ?? 0;

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

  getVehicleById(id: number): Observable<Vehicle> {
    return this.http
      .get<RawVehicleApi>(`${API_URL}/${id}`)
      .pipe(map((item) => this.normalizeVehicle(item)));
  }

  getPhotos(vehicleId: number): Observable<VehiclePhoto[]> {
    return this.http.get<VehiclePhoto[]>(`${API_URL}/${vehicleId}/photos`);
  }

  getAvailability(vehicleId: number, from: string, to: string): Observable<AvailabilityResponse> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<AvailabilityResponse>(`${API_URL}/${vehicleId}/availability`, { params });
  }

  create(request: VehicleCreateRequest): Observable<Vehicle> {
    return this.http
      .post<RawVehicleApi>(API_URL, request)
      .pipe(map((item) => this.normalizeVehicle(item)));
  }

  uploadPhotos(vehicleId: number, files: File[]): Observable<VehiclePhoto[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<VehiclePhoto[]>(`${API_URL}/${vehicleId}/photos`, formData);
  }

  setPrimaryPhoto(vehicleId: number, photoId: number): Observable<VehiclePhoto> {
    return this.http.put<VehiclePhoto>(`${API_URL}/${vehicleId}/photos/${photoId}/primary`, {});
  }

  deletePhoto(vehicleId: number, photoId: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${vehicleId}/photos/${photoId}`);
  }

  getBrands(): Observable<CatalogEntry[]> {
    return this.http.get<CatalogEntry[]>(BRANDS_API_URL);
  }

  getCategories(): Observable<CatalogEntry[]> {
    return this.http.get<CatalogEntry[]>(CATEGORIES_API_URL);
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
    const primaryPhoto = item.photos?.find((p) => p.isPrimary);
    const resolvedPhotoUrl =
      item.photoUrl ??
      item.primaryPhotoUrl ??
      primaryPhoto?.thumbnailUrl ??
      primaryPhoto?.url ??
      null;

    let photos = item.photos ?? [];
    if (photos.length === 0 && resolvedPhotoUrl) {
      photos = [
        {
          id: 1,
          vehicleId: item.id,
          url: resolvedPhotoUrl,
          isPrimary: true,
        },
      ];
    }

    return {
      ...item,
      photoUrl: resolvedPhotoUrl,
      photos,
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
