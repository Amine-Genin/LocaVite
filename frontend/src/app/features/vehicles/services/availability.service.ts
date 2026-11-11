import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AvailabilityResponse } from '../models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/vehicles';

  // Cache mémoire par clé `${vehicleId}-${from}-${to}`
  private readonly cache = new Map<string, AvailabilityResponse>();

  getAvailability(
    vehicleId: number,
    from?: string,
    to?: string,
  ): Observable<AvailabilityResponse> {
    const fromStr = from || '';
    const toStr = to || '';
    const cacheKey = `${vehicleId}-${fromStr}-${toStr}`;

    if (this.cache.has(cacheKey)) {
      return of(this.cache.get(cacheKey)!);
    }

    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http
      .get<AvailabilityResponse>(`${this.baseUrl}/${vehicleId}/availability`, { params })
      .pipe(
        tap((res) => {
          this.cache.set(cacheKey, res);
        }),
      );
  }

  clearCache(): void {
    this.cache.clear();
  }
}
