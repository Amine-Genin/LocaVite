import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  SaleListing,
  SaleListingCreateRequest,
  SalePriceEstimate,
  SalePriceEstimateFilter,
} from '../models/sale-listing.model';

@Injectable({ providedIn: 'root' })
export class SaleListingService {
  private readonly http = inject(HttpClient);

  create(request: SaleListingCreateRequest): Observable<SaleListing> {
    return this.http.post<SaleListing>('/api/sale-listings', request);
  }

  estimatePrice(filter: SalePriceEstimateFilter): Observable<SalePriceEstimate> {
    return this.http.post<SalePriceEstimate>('/api/sale-listings/estimate', filter);
  }
}
