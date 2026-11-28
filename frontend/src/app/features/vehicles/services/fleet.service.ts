import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export type FleetStatus = 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'MAINTENANCE' | 'SOLD' | 'ARCHIVED';

export interface FleetFilter {
  statut?: string;
  categorie?: string;
  ville?: string;
}

export interface FleetVehicle {
  id: number;
  matricule: string;
  marque: string;
  modele: string;
  annee: number;
  categorie: string;
  statut: FleetStatus;
  ville: string;
  localisationActuelle: string;
  prochaineReservationId?: number;
  prochaineReservation?: string;
  kilometrage: number;
  derniereMaintenance?: string;
  prochaineMaintenance?: string;
  revenusMois: number;
  revenusAnnee: number;
  tauxOccupation: number;
  agenceId: number;
  createdAt?: string;
  photoUrl?: string;
}

export interface FleetHistory {
  type: string;
  description?: string;
  dateHeure?: string;
  date?: string;
  ancienStatut?: string;
  nouveauStatut?: string;
  kilometrage?: number;
  referenceId?: number;
}

const FALLBACK_FLEET_PHOTOS = [
  'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1609521263047-f8f205293f24?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?auto=format&fit=crop&w=400&q=75',
  'https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=400&q=75',
];

@Injectable({ providedIn: 'root' })
export class FleetService {
  private readonly http = inject(HttpClient);

  getAll(filter: FleetFilter = {}): Observable<FleetVehicle[]> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value) {
        params = params.set(key, value);
      }
    });
    return this.http.get<FleetVehicle[]>('/api/admin/fleet', { params }).pipe(
      map((vehicles) => (vehicles ?? []).map((v) => this.normalizeVehicle(v))),
    );
  }

  getFleet(filter: FleetFilter = {}): Observable<FleetVehicle[]> {
    return this.getAll(filter);
  }

  updateStatus(id: number, statut: FleetStatus, raison = ''): Observable<FleetVehicle> {
    return this.http
      .put<FleetVehicle>(`/api/admin/vehicles/${id}/status`, { statut, raison })
      .pipe(map((v) => this.normalizeVehicle(v)));
  }

  getHistory(id: number): Observable<FleetHistory[]> {
    return this.http.get<FleetHistory[]>(`/api/admin/vehicles/${id}/history`);
  }

  addMaintenance(
    id: number,
    payload: { date: string; description: string; kilometrage: number; prochaineMaintenance?: string },
  ): Observable<FleetHistory> {
    return this.http.post<FleetHistory>(`/api/admin/vehicles/${id}/maintenance`, payload);
  }

  addIncident(
    id: number,
    payload: { date: string; description: string; gravite: string },
  ): Observable<FleetHistory> {
    return this.http.post<FleetHistory>(`/api/admin/vehicles/${id}/incidents`, payload);
  }

  transfer(id: number, agenceId: number): Observable<FleetVehicle> {
    return this.http
      .put<FleetVehicle>(`/api/admin/vehicles/${id}/agency`, { agenceId })
      .pipe(map((v) => this.normalizeVehicle(v)));
  }

  getFallbackPhoto(vehicleId: number): string {
    const idx = Math.abs((vehicleId || 1) - 1) % FALLBACK_FLEET_PHOTOS.length;
    return FALLBACK_FLEET_PHOTOS[idx];
  }

  private normalizeVehicle(vehicle: FleetVehicle): FleetVehicle {
    return {
      ...vehicle,
      photoUrl: vehicle.photoUrl || this.getFallbackPhoto(vehicle.id),
    };
  }
}
