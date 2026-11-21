import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ContactRequestBody {
  nom: string;
  email: string;
  telephone: string;
  message: string;
}

export interface ContactRequestResponse {
  id: number;
  saleListingId: number;
}

interface SaleListingPage {
  content: { id: number }[];
}

@Injectable({ providedIn: 'root' })
export class SaleListingContactService {
  private readonly http = inject(HttpClient);

  getPublishedListingIdByVehicle(vehicleId: number): Observable<number | null> {
    const params = new HttpParams()
      .set('vehicleId', String(vehicleId))
      .set('statut', 'PUBLISHED')
      .set('page', '0')
      .set('size', '1');
    return this.http
      .get<SaleListingPage>('/api/sale-listings', { params })
      .pipe(map((page) => page.content[0]?.id ?? null));
  }

  sendContactRequest(
    listingId: number,
    body: ContactRequestBody,
  ): Observable<ContactRequestResponse> {
    return this.http.post<ContactRequestResponse>(`/api/sale-listings/${listingId}/contact`, body);
  }
}
