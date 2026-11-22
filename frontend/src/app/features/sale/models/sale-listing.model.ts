export type SaleCondition = 'NEW' | 'USED';
export type SaleFuel = 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE';

export interface SaleListing {
  id: number;
  vehicleId: number;
  titre: string;
  description?: string | null;
  prixVente: number;
  prixNego?: boolean | null;
  etat: SaleCondition;
  kilometrage: number | null;
  numeroChassis?: string | null;
  nombreProprietaires?: number | null;
  puissance?: number | null;
  dateMiseEnCirculation?: string | null;
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
