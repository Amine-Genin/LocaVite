import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PriceQuote,
  Reservation,
  ReservationFilterParams,
  ReservationPageResponse,
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

  getMyReservations(page = 0, size = 20): Observable<ReservationPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<ReservationPageResponse>(`${this.apiUrl}/me`, { params });
  }

  getAll(filterParams: ReservationFilterParams = {}): Observable<ReservationPageResponse> {
    let params = new HttpParams();
    if (filterParams.page !== undefined) params = params.set('page', filterParams.page.toString());
    if (filterParams.size !== undefined) params = params.set('size', filterParams.size.toString());
    if (filterParams.sort) params = params.set('sort', filterParams.sort);
    if (filterParams.statut) params = params.set('statut', filterParams.statut);
    if (filterParams.code) params = params.set('code', filterParams.code);
    if (filterParams.userId) params = params.set('userId', filterParams.userId.toString());
    if (filterParams.vehicleId) params = params.set('vehicleId', filterParams.vehicleId.toString());
    if (filterParams.dateDebut) params = params.set('dateDebut', filterParams.dateDebut);
    if (filterParams.dateFin) params = params.set('dateFin', filterParams.dateFin);

    return this.http.get<ReservationPageResponse>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Reservation> {
    return this.http.get<Reservation>(`${this.apiUrl}/${id}`);
  }

  confirm(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/confirm`, {});
  }

  complete(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/complete`, {});
  }

  cancel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  cancelWithRefund(id: number): Observable<unknown> {
    return this.http.post<unknown>(`${this.apiUrl}/${id}/cancel`, {});
  }

  downloadContract(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/contract`, {
      responseType: 'blob',
    });
  }

  extendReservation(id: number, nouvelleDateFin: string): Observable<unknown> {
    return this.http.post<unknown>(`${this.apiUrl}/${id}/extend`, { nouvelleDateFin });
  }
}
