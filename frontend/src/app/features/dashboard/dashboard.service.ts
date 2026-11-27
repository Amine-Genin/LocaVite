import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { ReservationPageResponse } from '../reservations/models/reservation.model';
import { PaymentPageResponse } from '../payments/models/payment.model';
import { VehiclePageResponse } from '../vehicles/models/vehicle.model';
import { SaleListingPage } from '../sale/models/sale-listing.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getKpis(): Observable<{
    reservations: ReservationPageResponse;
    payments: PaymentPageResponse;
    vehicles: VehiclePageResponse;
    sales: SaleListingPage;
  }> {
    const params = new HttpParams().set('page', '0').set('size', '500').set('sort', 'createdAt,desc');
    return forkJoin({
      reservations: this.http.get<ReservationPageResponse>('/api/reservations', { params }),
      payments: this.http.get<PaymentPageResponse>('/api/payments', { params }),
      vehicles: this.http.get<VehiclePageResponse>('/api/vehicles', { params }),
      sales: this.http.get<SaleListingPage>('/api/sale-listings', { params }),
    });
  }

  getCharts(): Observable<unknown> {
    return this.http.get('/api/admin/contact-requests');
  }
}
