import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { FleetHistory, FleetService, FleetStatus, FleetVehicle } from '../services/fleet.service';

@Component({
  selector: 'app-admin-vehicle-list-placeholder', standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, TranslateModule],
  templateUrl: './admin-vehicle-list-placeholder.component.html',
  styleUrl: './admin-vehicle-list-placeholder.component.scss',
})
export class AdminVehicleListPlaceholderComponent implements OnInit {
  private readonly fleetService = inject(FleetService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  readonly vehicles = signal<FleetVehicle[]>([]);
  readonly history = signal<FleetHistory[]>([]);
  readonly selectedVehicle = signal<number | null>(null);
  readonly loading = signal(true);
  readonly filter = signal({ statut: '', categorie: '', ville: '' });
  readonly isAdmin = this.auth.hasRole('ADMIN');
  readonly statuses: FleetStatus[] = ['PUBLISHED', 'RENTED', 'MAINTENANCE', 'ARCHIVED'];

  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading.set(true);
    this.fleetService.getFleet(this.filter()).subscribe({
      next: (items) => { this.vehicles.set(items ?? []); this.loading.set(false); },
      error: () => { this.loading.set(false); this.snackBar.open('Impossible de charger la flotte.', 'Fermer', { duration: 3500 }); },
    });
  }
  setFilter(key: 'statut' | 'categorie' | 'ville', value: string): void { this.filter.update((f) => ({ ...f, [key]: value })); }
  reset(): void { this.filter.set({ statut: '', categorie: '', ville: '' }); this.load(); }
  applyFilters(): void { this.load(); }
  changeStatus(vehicle: FleetVehicle, value: string): void {
    if (!this.statuses.includes(value as FleetStatus)) return;
    const reason = window.prompt('Motif du changement de statut (facultatif) :') ?? '';
    this.fleetService.updateStatus(vehicle.id, value as FleetStatus, reason).subscribe({
      next: () => { this.snackBar.open('Statut du véhicule mis à jour.', 'OK', { duration: 2500 }); this.load(); if (this.selectedVehicle() === vehicle.id) this.showHistory(vehicle.id); },
      error: () => this.snackBar.open('La mise à jour du statut a échoué.', 'Fermer', { duration: 3500 }),
    });
  }
  showHistory(id: number): void {
    this.selectedVehicle.set(id); this.history.set([]);
    this.fleetService.getHistory(id).subscribe({ next: (items) => this.history.set(items ?? []), error: () => this.snackBar.open("Impossible de charger l'historique.", 'Fermer', { duration: 3000 }) });
  }
  closeHistory(): void { this.selectedVehicle.set(null); }
  addMaintenance(vehicle: FleetVehicle): void {
    const description = window.prompt('Description de la maintenance :');
    if (!description?.trim()) return;
    const mileageText = window.prompt('Kilométrage relevé :', String(vehicle.kilometrage));
    if (mileageText === null || !Number.isFinite(Number(mileageText)) || Number(mileageText) < 0) return;
    const next = window.prompt('Date de la prochaine maintenance (AAAA-MM-JJ, facultatif) :') || undefined;
    this.fleetService.addMaintenance(vehicle.id, { date: new Date().toISOString().slice(0, 10), description: description.trim(), kilometrage: Number(mileageText), prochaineMaintenance: next }).subscribe({
      next: () => { this.snackBar.open('Maintenance enregistrée.', 'OK', { duration: 2500 }); this.load(); if (this.selectedVehicle() === vehicle.id) this.showHistory(vehicle.id); },
      error: () => this.snackBar.open("L'enregistrement de la maintenance a échoué.", 'Fermer', { duration: 3500 }),
    });
  }
  addIncident(vehicle: FleetVehicle): void {
    const description = window.prompt('Description de l’incident :');
    if (!description?.trim()) return;
    const gravite = window.prompt('Gravité (LOW, MEDIUM, HIGH) :', 'MEDIUM');
    if (!gravite?.trim()) return;
    this.fleetService.addIncident(vehicle.id, { date: new Date().toISOString().slice(0, 10), description: description.trim(), gravite: gravite.trim().toUpperCase() }).subscribe({
      next: () => { this.snackBar.open('Incident enregistré.', 'OK', { duration: 2500 }); if (this.selectedVehicle() === vehicle.id) this.showHistory(vehicle.id); },
      error: () => this.snackBar.open("L'enregistrement de l'incident a échoué.", 'Fermer', { duration: 3500 }),
    });
  }
  transfer(vehicle: FleetVehicle): void {
    const value = window.prompt('Identifiant de la nouvelle agence :');
    if (value === null || !Number.isInteger(Number(value)) || Number(value) <= 0) return;
    this.fleetService.transfer(vehicle.id, Number(value)).subscribe({
      next: () => { this.snackBar.open('Véhicule transféré.', 'OK', { duration: 2500 }); this.load(); },
      error: () => this.snackBar.open('Le transfert a échoué. Vérifiez l’agence cible.', 'Fermer', { duration: 3500 }),
    });
  }
  exportCsv(): void {
    const headers = ['Immatriculation','Véhicule','Catégorie','Statut','Ville','Localisation','Prochaine réservation','Kilométrage','Dernière maintenance','Prochaine maintenance','Revenus mois (MAD)','Revenus année (MAD)','Occupation (%)'];
    const rows = this.vehicles().map(v => [v.matricule, `${v.marque} ${v.modele} ${v.annee}`, v.categorie, v.statut, v.ville, v.localisationActuelle, v.prochaineReservation ?? '', v.kilometrage, v.derniereMaintenance ?? '', v.prochaineMaintenance ?? '', v.revenusMois, v.revenusAnnee, v.tauxOccupation]);
    const csv = '\uFEFF' + [headers, ...rows].map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(';')).join('\r\n');
    const link = document.createElement('a'); link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    link.download = `flotte-locavite-${new Date().toISOString().slice(0, 10)}.csv`; link.click(); URL.revokeObjectURL(link.href);
  }
  trackById(_: number, vehicle: FleetVehicle): number { return vehicle.id; }
}
