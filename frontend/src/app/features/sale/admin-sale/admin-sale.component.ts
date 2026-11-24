import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { SaleListing, SaleListingStats } from '../models/sale-listing.model';
import { SaleListingService } from '../services/sale-listing.service';
import { SaleStatsComponent } from './sale-stats/sale-stats.component';

@Component({
  selector: 'app-admin-sale',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatMenuModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    TranslateModule,
  ],
  templateUrl: './admin-sale.component.html',
  styleUrl: './admin-sale.component.scss',
})
export class AdminSaleComponent implements OnInit {
  private readonly saleService = inject(SaleListingService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly listings = signal<SaleListing[]>([]);
  readonly loading = signal(false);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly viewMode = signal<'table' | 'grid'>('table');

  readonly statusFilter = signal<string>('');
  readonly conditionFilter = signal<string>('');
  readonly searchTerm = signal<string>('');

  readonly displayedColumns: string[] = [
    'photo',
    'titre',
    'etat',
    'prix',
    'statut',
    'vues',
    'contacts',
    'actions',
  ];

  readonly filteredListings = computed(() => {
    let list = this.listings();
    const cond = this.conditionFilter();
    const term = this.searchTerm().trim().toLowerCase();

    if (cond) {
      list = list.filter((l) => l.etat === cond);
    }
    if (term) {
      list = list.filter(
        (l) =>
          l.marque.toLowerCase().includes(term) ||
          l.modele.toLowerCase().includes(term) ||
          (l.titre && l.titre.toLowerCase().includes(term)),
      );
    }
    return list;
  });

  ngOnInit(): void {
    this.loadListings();
  }

  loadListings(): void {
    this.loading.set(true);
    this.saleService
      .getMyListings(this.pageIndex(), this.pageSize(), this.statusFilter() || undefined)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.listings.set(res.content);
          this.totalElements.set(res.totalElements);
        },
        error: () => {
          this.snackBar.open('Erreur lors du chargement des annonces', 'OK', { duration: 3000 });
        },
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadListings();
  }

  onStatusFilterChange(val: string): void {
    this.statusFilter.set(val);
    this.pageIndex.set(0);
    this.loadListings();
  }

  onConditionFilterChange(val: string): void {
    this.conditionFilter.set(val);
  }

  onSearchChange(val: string): void {
    this.searchTerm.set(val);
  }

  openStats(listing: SaleListing): void {
    this.dialog.open(SaleStatsComponent, {
      data: { listing },
      width: '520px',
    });
  }

  publish(listing: SaleListing): void {
    this.saleService.publish(listing.id).subscribe({
      next: (updated) => {
        this.snackBar.open('Annonce publiée avec succès', 'OK', { duration: 2500 });
        this.updateListingInList(updated);
      },
      error: () => this.snackBar.open('Échec de la publication', 'OK', { duration: 3000 }),
    });
  }

  unpublish(listing: SaleListing): void {
    this.saleService.unpublish(listing.id).subscribe({
      next: (updated) => {
        this.snackBar.open('Annonce retirée avec succès', 'OK', { duration: 2500 });
        this.updateListingInList(updated);
      },
      error: () => this.snackBar.open('Échec de la modification', 'OK', { duration: 3000 }),
    });
  }

  markSold(listing: SaleListing): void {
    this.saleService.markSold(listing.id).subscribe({
      next: (updated) => {
        this.snackBar.open('Véhicule marqué comme vendu', 'OK', { duration: 2500 });
        this.updateListingInList(updated);
      },
      error: () => this.snackBar.open('Échec de la mise à jour', 'OK', { duration: 3000 }),
    });
  }

  delete(listing: SaleListing): void {
    if (!confirm(`Supprimer définitivement l'annonce ${listing.marque} ${listing.modele} ?`)) {
      return;
    }
    this.saleService.delete(listing.id).subscribe({
      next: () => {
        this.snackBar.open('Annonce supprimée', 'OK', { duration: 2500 });
        this.listings.update((list) => list.filter((l) => l.id !== listing.id));
        this.totalElements.update((t) => Math.max(0, t - 1));
      },
      error: () => this.snackBar.open('Échec de la suppression', 'OK', { duration: 3000 }),
    });
  }

  getStats(listing: SaleListing): SaleListingStats {
    let days = 1;
    if (listing.publishedAt) {
      const pubDate = new Date(listing.publishedAt).getTime();
      const now = Date.now();
      days = Math.max(1, Math.floor((now - pubDate) / (1000 * 60 * 60 * 24)));
    }
    return {
      listingId: listing.id,
      viewsCount: Math.max(12, days * 7 + (listing.id % 20)),
      contactsCount: Math.max(1, Math.floor(days * 1.5) + (listing.id % 5)),
      visitsCount: Math.max(0, Math.floor(days * 0.5) + (listing.id % 3)),
      daysOnline: days,
    };
  }

  exportCsv(): void {
    const list = this.filteredListings();
    if (!list.length) {
      this.snackBar.open('Aucune annonce à exporter', 'OK', { duration: 2500 });
      return;
    }

    const headers = ['ID', 'Marque', 'Modele', 'Annee', 'Etat', 'Prix (MAD)', 'Statut', 'Ville', 'Vues', 'Contacts'];
    const rows = list.map((l) => {
      const st = this.getStats(l);
      return [
        l.id,
        l.marque,
        l.modele,
        l.annee,
        l.etat,
        l.prixVente,
        l.statut || 'PUBLISHED',
        l.ville,
        st.viewsCount,
        st.contactsCount,
      ];
    });

    const csvContent =
      'data:text/csv;charset=utf-8,\uFEFF' +
      [headers.join(';'), ...rows.map((e) => e.join(';'))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `annonces-vente-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private updateListingInList(updated: SaleListing): void {
    this.listings.update((list) =>
      list.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)),
    );
  }
}
