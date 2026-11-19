export type HistoryEventType =
  | 'MAINTENANCE'
  | 'REPARATION'
  | 'ACCIDENT'
  | 'CONTROLE_TECHNIQUE'
  | 'VIDANGE'
  | 'CHANGEMENT_PNEUS'
  | 'AUTRE';

export interface VehicleHistoryEvent {
  id: number;
  saleListingId: number;
  vehicleId: number;
  typeEvenement: HistoryEventType;
  date: string;
  description: string;
  kilometrage: number;
  documentUrl?: string | null;
  verified: boolean;
}

export interface VehicleHistoryLookupResult {
  hasListing: boolean;
  events: VehicleHistoryEvent[];
}
