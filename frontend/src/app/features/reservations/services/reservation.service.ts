import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PriceQuote,
  Reservation,
  ReservationRequest,
  QuoteRequest,
} from '../models/reservation.model';
import { AvailabilityResponse } from '../../vehicles/models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/reservations';

  getQuote(request: QuoteRequest): Observable<PriceQuote> {
    return this.http.post<PriceQuote>(`${this.apiUrl}/quote`, request);
  }

  checkAvailability(
    vehicleId: number,
    from: string,
    to: string,
  ): Observable<AvailabilityResponse> {
    return this.http.get<AvailabilityResponse>(`/api/vehicles/${vehicleId}/availability`, {
      params: { from, to },
    });
  }

  create(request: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.apiUrl, request);
  }
}
