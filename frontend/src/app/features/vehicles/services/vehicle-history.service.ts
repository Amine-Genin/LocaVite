import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, of, switchMap } from 'rxjs';

import { VehicleHistoryEvent, VehicleHistoryLookupResult } from '../models/vehicle-history.model';

interface SaleListingSummary {
  id: number;
  etat: 'NEW' | 'USED';
  statut: string;
}

interface SaleListingPage {
  content?: SaleListingSummary[];
}

@Injectable({ providedIn: 'root' })
export class VehicleHistoryService {
  private readonly http = inject(HttpClient);

  getHistoryForVehicle(vehicleId: number): Observable<VehicleHistoryLookupResult> {
    const params = new HttpParams()
      .set('vehicleId', String(vehicleId))
      .set('etat', 'USED')
      .set('statut', 'PUBLISHED')
      .set('size', '100');

    return this.http.get<SaleListingPage>('/api/sale-listings', { params }).pipe(
      map((page) => page.content?.find((listing) => listing.etat === 'USED')),
      switchMap((listing) => {
        if (!listing) {
          return of({ hasListing: false, events: [] });
        }
        return this.http
          .get<VehicleHistoryEvent[]>(`/api/sale-listings/${listing.id}/history`)
          .pipe(map((events) => ({ hasListing: true, events })));
      }),
    );
  }
}
