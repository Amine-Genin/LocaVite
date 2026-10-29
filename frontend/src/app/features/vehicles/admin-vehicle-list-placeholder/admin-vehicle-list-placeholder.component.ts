import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-admin-vehicle-list-placeholder',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="admin-vehicles-page" data-cy="admin-vehicles-page">
      <header class="page-header">
        <h1 data-cy="admin-vehicles-title">Gestion de la flotte de véhicules</h1>
        <a
          mat-raised-button
          color="primary"
          routerLink="/admin/vehicles/new"
          data-cy="add-vehicle-nav-btn"
        >
          <mat-icon>add</mat-icon>
          Ajouter un véhicule
        </a>
      </header>
      <div class="page-content" data-cy="admin-vehicles-content">
        <p>Bienvenue dans votre catalogue agence / admin.</p>
      </div>
    </div>
  `,
  styles: [
    `
      .admin-vehicles-page {
        max-width: 1200px;
        margin: 2rem auto;
        padding: 0 1rem;
      }
      .page-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
      }
      .page-header h1 {
        margin: 0;
        font-size: 1.75rem;
        font-weight: 700;
        color: #1e293b;
      }
    `,
  ],
})
export class AdminVehicleListPlaceholderComponent {}

