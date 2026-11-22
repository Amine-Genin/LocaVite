import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type SaleVisitType = 'VISITE_SIMPLE' | 'ESSAI_ROUTIER' | 'INSPECTION_MECANIQUE';

export interface SaleVisitRequest {
  dateSouhaitee: string;
  creneauHoraire: string;
  lieu: string;
  typeVisite: SaleVisitType;
}

export interface SaleVisitResponse {
  id: number;
  saleListingId: number;
  dateSouhaitee: string;
  creneauHoraire: string;
  lieu: string;
  typeVisite: SaleVisitType;
  statut: string;
}

@Injectable({ providedIn: 'root' })
export class SaleVisitService {
  private readonly http = inject(HttpClient);

  request(listingId: number, body: SaleVisitRequest): Observable<SaleVisitResponse> {
    return this.http.post<SaleVisitResponse>(
      `/api/sale-listings/${listingId}/visit-request`,
      body,
    );
  }
}
