import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-admin-sale-list-placeholder',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <main class="admin-sale-page" data-cy="admin-sale-page">
      <header>
        <h1>Gestion des annonces de vente</h1>
        <a mat-raised-button color="primary" routerLink="/admin/sale/new" data-cy="add-sale-button">
          <mat-icon>add</mat-icon>
          Publier une annonce
        </a>
      </header>
      <p>Les annonces créées apparaîtront ici.</p>
    </main>
  `,
  styles: [
    `
      .admin-sale-page { max-width: 1200px; margin: 2rem auto; padding: 0 1rem; }
      header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
      h1 { margin: 0; }
      p { margin-top: 2rem; color: #64748b; }
    `,
  ],
})
export class AdminSaleListPlaceholderComponent {}
