export type SaleCondition = 'NEW' | 'USED';
export type SaleFuel = 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE';
export type SaleStatus = 'DRAFT' | 'PUBLISHED' | 'SOLD' | 'ARCHIVED';

export interface SaleListing {
  id: number;
  vehicleId: number;
  titre: string;
  description?: string | null;
  prixVente: number;
  prixNego?: boolean | null;
  etat: SaleCondition;
  kilometrage: number | null;
  premierProprietaire?: boolean | null;
  dateMiseEnCirculation?: string | null;
  numeroChassis?: string | null;
  nombreProprietaires?: number | null;
  puissance?: number | null;
  statut?: SaleStatus;
  marque: string;
  modele: string;
  annee: number;
  ville: string;
  carburant: SaleFuel | string;
  transmission?: string | null;
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
  statut: 'DRAFT' | 'PUBLISHED';
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

export interface SaleListingFilters {
  etat?: SaleCondition;
  marque?: string;
  modele?: string;
  categorie?: string;
  prixMin?: number;
  prixMax?: number;
  anneeMin?: number;
  anneeMax?: number;
  kilometrageMax?: number;
  carburant?: SaleFuel;
  ville?: string;
  page: number;
  size: number;
  sort: string;
}

export interface SaleListingPage {
  content: SaleListing[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface SaleListingStats {
  listingId: number;
  viewsCount: number;
  contactsCount: number;
  visitsCount: number;
  daysOnline: number;
}
