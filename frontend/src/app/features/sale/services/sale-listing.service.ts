import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  SaleListing,
  SaleListingCreateRequest,
  SaleListingFilters,
  SaleListingPage,
  SalePriceEstimate,
  SalePriceEstimateFilter,
} from '../models/sale-listing.model';

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

  create(request: SaleListingCreateRequest): Observable<SaleListing> {
    return this.http.post<SaleListing>('/api/sale-listings', request);
  }

  estimatePrice(filter: SalePriceEstimateFilter): Observable<SalePriceEstimate> {
    return this.http.post<SalePriceEstimate>('/api/sale-listings/estimate', filter);
  }

  getById(id: number): Observable<SaleListing> {
    return this.http.get<SaleListing>(`/api/sale-listings/${id}`);
  }

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

  getMyListings(page = 0, size = 10, statut?: string): Observable<SaleListingPage> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (statut) {
      params = params.set('statut', statut);
    }
    return this.http.get<SaleListingApiPage>('/api/sale-listings', { params }).pipe(
      map((response) => {
        const content = response.content ?? [];
        const number = response.page?.number ?? response.number ?? page;
        const s = response.page?.size ?? response.size ?? size;
        const totalElements =
          response.page?.totalElements ?? response.totalElements ?? content.length;
        const totalPages =
          response.page?.totalPages ??
          response.totalPages ??
          (s > 0 ? Math.ceil(totalElements / s) : 0);

        return { content, number, size: s, totalElements, totalPages };
      }),
    );
  }

  publish(id: number): Observable<SaleListing> {
    return this.http.put<SaleListing>(`/api/sale-listings/${id}/publish`, {});
  }

  unpublish(id: number): Observable<SaleListing> {
    return this.http.put<SaleListing>(`/api/sale-listings/${id}`, { statut: 'DRAFT' });
  }

  markSold(id: number): Observable<SaleListing> {
    return this.http.put<SaleListing>(`/api/sale-listings/${id}/mark-sold`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/sale-listings/${id}`);
  }
}
