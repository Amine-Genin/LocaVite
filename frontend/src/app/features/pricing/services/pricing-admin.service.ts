import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReservationOption } from '../../reservations/models/reservation.model';

export interface Season {
  id: number;
  nom: string;
  dateDebut: string;
  dateFin: string;
  coefficient: number;
  priorite: number;
  agenceId?: number | null;
  vehicleId?: number | null;
  isRecurring?: boolean;
}

export interface SeasonRequest {
  nom: string;
  dateDebut: string;
  dateFin: string;
  coefficient: number;
  priorite: number;
  agenceId?: number | null;
  vehicleId?: number | null;
  isRecurring?: boolean;
}

export interface OptionPrice {
  id: number;
  type: ReservationOption;
  libelle: string;
  prixJour: number;
  actif: boolean;
}

export interface OptionPriceRequest {
  type: ReservationOption;
  libelle: string;
  prixJour: number;
  actif: boolean;
}

export type DiscountType = 'LONG_DURATION' | 'PROMO_CODE';

export interface Discount {
  id: number;
  type: DiscountType;
  code?: string | null;
  libelle: string;
  minDays?: number | null;
  pourcentage: number;
  dateDebut?: string | null;
  dateFin?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DiscountRequest {
  type: DiscountType;
  code?: string | null;
  libelle: string;
  minDays?: number | null;
  pourcentage: number;
  dateDebut?: string | null;
  dateFin?: string | null;
  actif: boolean;
}

export interface PricingSimulateRequest {
  vehicleId?: number | null;
  prixJour?: number | null;
  caution?: number | null;
  agenceId?: number | null;
  dateDebut: string;
  dateFin: string;
  options: ReservationOption[];
  promoCode?: string | null;
}

export interface PricingSimulateResponse {
  sousTotal: number;
  options: number;
  remiseDuree: number;
  remisePromo: number;
  remiseTotale: number;
  caution: number;
  tva: number;
  total: number;
  jours: number;
  promoCodeApplique?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PricingAdminService {
  private readonly http = inject(HttpClient);

  // Seasons
  getSeasons(): Observable<Season[]> {
    return this.http.get<Season[]>('/api/admin/seasons');
  }

  createSeason(request: SeasonRequest): Observable<Season> {
    return this.http.post<Season>('/api/admin/seasons', request);
  }

  updateSeason(id: number, request: SeasonRequest): Observable<Season> {
    return this.http.put<Season>(`/api/admin/seasons/${id}`, request);
  }

  deleteSeason(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/seasons/${id}`);
  }

  // Options
  getOptions(): Observable<OptionPrice[]> {
    return this.http.get<OptionPrice[]>('/api/admin/options');
  }

  createOption(request: OptionPriceRequest): Observable<OptionPrice> {
    return this.http.post<OptionPrice>('/api/admin/options', request);
  }

  updateOption(id: number, request: OptionPriceRequest): Observable<OptionPrice> {
    return this.http.put<OptionPrice>(`/api/admin/options/${id}`, request);
  }

  deleteOption(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/options/${id}`);
  }

  // Discounts
  getDiscounts(): Observable<Discount[]> {
    return this.http.get<Discount[]>('/api/admin/discounts');
  }

  createDiscount(request: DiscountRequest): Observable<Discount> {
    return this.http.post<Discount>('/api/admin/discounts', request);
  }

  updateDiscount(id: number, request: DiscountRequest): Observable<Discount> {
    return this.http.put<Discount>(`/api/admin/discounts/${id}`, request);
  }

  deleteDiscount(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/discounts/${id}`);
  }

  // Simulator
  simulatePrice(request: PricingSimulateRequest): Observable<PricingSimulateResponse> {
    return this.http.post<PricingSimulateResponse>('/api/admin/pricing/simulate', request);
  }
}
