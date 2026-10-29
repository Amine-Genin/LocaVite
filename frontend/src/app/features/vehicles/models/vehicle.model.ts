export type FuelType = 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE';
export type TransmissionType = 'MANUELLE' | 'AUTOMATIQUE';
export type VehicleStatus = 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'SOLD' | 'ARCHIVED';

export interface CatalogEntry {
  id: number;
  name: string;
}

export interface VehiclePhoto {
  id: number;
  vehicleId: number;
  url: string;
  thumbnailUrl?: string;
  isPrimary: boolean;
  ordre?: number;
  uploadedAt?: string;
}

export interface Vehicle {
  id: number;
  brandId?: number;
  marque: string;
  modele: string;
  annee: number;
  categoryId?: number;
  categorie: string;
  ville: string;
  prixJour: number;
  prixVente?: number;
  carburant?: FuelType;
  transmission?: TransmissionType;
  places?: number;
  description?: string;
  matricule?: string;
  statut?: VehicleStatus;
  agenceId?: number;
  agenceNom?: string;
  agenceTelephone?: string;
  agenceEmail?: string;
  agenceAdresse?: string;
  photoUrl?: string | null;
  photos?: VehiclePhoto[];
  isFavorite?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface VehicleCreateRequest {
  brandId: number;
  modele: string;
  annee: number;
  categoryId: number;
  ville: string;
  prixJour: number;
  prixVente: number;
  carburant: FuelType;
  transmission: TransmissionType;
  places: number;
  description: string;
  matricule: string;
  statut?: VehicleStatus;
}

export interface CatalogEntry {
  id: number;
  name: string;
}

export interface UploadedPhotoItem {
  id?: number;
  file?: File;
  previewUrl: string;
  isPrimary: boolean;
  ordre?: number;
  name?: string;
  size?: number;
}

export interface AvailabilityPeriod {
  from: string;
  to: string;
}

export interface UnavailablePeriod {
  from: string;
  to: string;
  reason?: string;
}

export interface AvailabilityResponse {
  available: AvailabilityPeriod[];
  unavailable: UnavailablePeriod[];
}

export interface VehicleFilter {
  marque?: string;
  categorie?: string;
  ville?: string;
  carburant?: FuelType | '';
  transmission?: TransmissionType | '';
  brandId?: number;
  categoryId?: number;
  dateDebut?: string;
  dateFin?: string;
  prixMin?: number;
  prixMax?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface VehiclePageResponse {
  content: Vehicle[];
  page?: PageMetadata;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
