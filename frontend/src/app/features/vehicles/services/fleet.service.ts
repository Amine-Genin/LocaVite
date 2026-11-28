import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type FleetStatus = 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'MAINTENANCE' | 'SOLD' | 'ARCHIVED';
export interface FleetVehicle {
  id: number; matricule: string; marque: string; modele: string; annee: number; categorie: string;
  statut: FleetStatus; ville: string; localisationActuelle: string; prochaineReservationId?: number;
  prochaineReservation?: string; kilometrage: number; derniereMaintenance?: string; prochaineMaintenance?: string;
  revenusMois: number; revenusAnnee: number; tauxOccupation: number; agenceId: number; createdAt?: string;
}
export interface FleetHistory { type: string; description?: string; dateHeure?: string; date?: string; ancienStatut?: string; nouveauStatut?: string; kilometrage?: number; referenceId?: number; }

@Injectable({ providedIn: 'root' })
export class FleetService {
  private readonly http = inject(HttpClient);
  getFleet(filter: { statut?: string; categorie?: string; ville?: string } = {}): Observable<FleetVehicle[]> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => { if (value) params = params.set(key, value); });
    return this.http.get<FleetVehicle[]>('/api/admin/fleet', { params });
  }
  updateStatus(id: number, statut: FleetStatus, raison = ''): Observable<FleetVehicle> {
    return this.http.put<FleetVehicle>(`/api/admin/vehicles/${id}/status`, { statut, raison });
  }
  getHistory(id: number): Observable<FleetHistory[]> { return this.http.get<FleetHistory[]>(`/api/admin/vehicles/${id}/history`); }
  addMaintenance(id: number, payload: { date: string; description: string; kilometrage: number; prochaineMaintenance?: string }): Observable<FleetHistory> {
    return this.http.post<FleetHistory>(`/api/admin/vehicles/${id}/maintenance`, payload);
  }
  addIncident(id: number, payload: { date: string; description: string; gravite: string }): Observable<FleetHistory> {
    return this.http.post<FleetHistory>(`/api/admin/vehicles/${id}/incidents`, payload);
  }
  transfer(id: number, agenceId: number): Observable<FleetVehicle> {
    return this.http.put<FleetVehicle>(`/api/admin/vehicles/${id}/agency`, { agenceId });
  }
}
