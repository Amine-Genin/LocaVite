export type ReservationOption = 'GPS' | 'BABY_SEAT' | 'EXTRA_DRIVER';

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

export interface Reservation {
  id: number;
  code: string;
  statut: string;
  prixTotal: number;
}
