import { Vehicle } from '../../vehicles/models/vehicle.model';

export type ReservationOption = 'GPS' | 'BABY_SEAT' | 'EXTRA_DRIVER';

export type ReservationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'CANCELLED';

export type CautionStatus = 'PENDING' | 'BLOCKED' | 'RELEASED' | 'FORFEITED';

export interface QuoteRequest {
  vehicleId: number;
  dateDebut: string;
  dateFin: string;
  options: ReservationOption[];
}

export interface ReservationRequest extends QuoteRequest {
  lieuPriseEnCharge: string;
  lieuRetour: string;
}

export interface PriceQuote {
  sousTotal: number;
  options: number;
  remise: number;
  caution: number;
  tva: number;
  total: number;
  jours: number;
}

export interface ReservationExtension {
  id?: number;
  nouvelleDateFin: string;
  prixSupplementaire: number;
  statut?: string;
  createdAt?: string;
}

export interface Reservation {
  id: number;
  code: string;
  userId?: number;
  clientNom?: string;
  clientEmail?: string;
  vehicleId?: number;
  vehicle?: Vehicle;
  dateDebut?: string;
  dateFin?: string;
  lieuPriseEnCharge?: string;
  lieuRetour?: string;
  prixTotal: number;
  caution?: number;
  cautionStatut?: CautionStatus;
  statut: ReservationStatus | string;
  options?: ReservationOption[];
  extensions?: ReservationExtension[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ReservationFilterParams {
  userId?: number;
  vehicleId?: number;
  statut?: ReservationStatus | string;
  code?: string;
  dateDebut?: string;
  dateFin?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ReservationPageResponse {
  content: Reservation[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}
