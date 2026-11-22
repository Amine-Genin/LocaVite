export type SaleCondition = 'NEW' | 'USED';
export type SaleStatus = 'DRAFT' | 'PUBLISHED';

export interface SaleListing {
  id: number;
  vehicleId: number;
  titre: string;
  description: string;
  prixVente: number;
  prixNego: boolean;
  etat: SaleCondition;
  kilometrage: number;
  premierProprietaire: boolean;
  dateMiseEnCirculation: string | null;
  numeroChassis: string | null;
  nombreProprietaires: number;
  statut: SaleStatus;
  marque: string;
  modele: string;
  annee: number;
  ville: string;
  carburant: string;
  transmission: string | null;
  categorie: string;
  photoUrl: string | null;
  publishedAt: string | null;
}

export interface SaleListingCreateRequest {
  vehicleId: number;
  titre: string;
  description: string;
  prixVente: number;
  prixNego: boolean;
  etat: SaleCondition;
  kilometrage: number;
  premierProprietaire: boolean;
  dateMiseEnCirculation: string | null;
  numeroChassis: string | null;
  nombreProprietaires: number;
  statut: SaleStatus;
}

export interface SalePriceEstimateFilter {
  marque?: string;
  modele?: string;
  categorie?: string;
  etat?: SaleCondition;
  anneeMin?: number;
  anneeMax?: number;
  kilometrageMax?: number;
  carburant?: string;
  ville?: string;
}

export interface SalePriceEstimate {
  estimatedPrice: number | null;
  minimumPrice: number | null;
  maximumPrice: number | null;
  comparableListings: number;
}
