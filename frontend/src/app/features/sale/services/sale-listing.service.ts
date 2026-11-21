import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { SaleListing, SaleListingFilters, SaleListingPage } from '../models/sale-listing.model';

interface SaleListingApiPage {
  content?: SaleListing[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: {
    totalElements?: number;
    totalPages?: number;
    number?: number;
    size?: number;
  };
}

@Injectable({ providedIn: 'root' })
export class SaleListingService {
  private readonly http = inject(HttpClient);

  search(filters: SaleListingFilters): Observable<SaleListingPage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }

    return this.http.get<SaleListingApiPage>('/api/sale-listings/search', { params }).pipe(
      map((response) => {
        const content = response.content ?? [];
        const number = response.page?.number ?? response.number ?? filters.page;
        const size = response.page?.size ?? response.size ?? filters.size;
        const totalElements =
          response.page?.totalElements ?? response.totalElements ?? content.length;
        const totalPages =
          response.page?.totalPages ??
          response.totalPages ??
          (size > 0 ? Math.ceil(totalElements / size) : 0);

        return { content, number, size, totalElements, totalPages };
      }),
    );
  }
}
