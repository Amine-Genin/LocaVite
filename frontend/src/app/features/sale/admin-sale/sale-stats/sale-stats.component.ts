import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';
import { SaleListing, SaleListingStats } from '../../models/sale-listing.model';

export interface SaleStatsDialogData {
  listing: SaleListing;
  stats?: SaleListingStats;
}

@Component({
  selector: 'app-sale-stats',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatCardModule, TranslateModule],
  template: `
    <div class="stats-dialog" data-cy="sale-stats-dialog">
      <h2 mat-dialog-title>
        <mat-icon color="primary">insights</mat-icon>
        {{ 'SALE.ADMIN.STATS_TITLE' | translate }}
      </h2>
      <mat-dialog-content>
        <p class="listing-subtitle">{{ data.listing.marque }} {{ data.listing.modele }} ({{ data.listing.annee }})</p>

        <div class="kpi-grid">
          <mat-card class="kpi-card" data-cy="stat-views">
            <div class="kpi-icon blue">
              <mat-icon>visibility</mat-icon>
            </div>
            <div class="kpi-info">
              <span class="label">{{ 'SALE.ADMIN.VIEWS' | translate }}</span>
              <span class="value">{{ stats.viewsCount }}</span>
            </div>
          </mat-card>

          <mat-card class="kpi-card" data-cy="stat-contacts">
            <div class="kpi-icon green">
              <mat-icon>chat</mat-icon>
            </div>
            <div class="kpi-info">
              <span class="label">{{ 'SALE.ADMIN.CONTACTS' | translate }}</span>
              <span class="value">{{ stats.contactsCount }}</span>
            </div>
          </mat-card>

          <mat-card class="kpi-card" data-cy="stat-visits">
            <div class="kpi-icon purple">
              <mat-icon>event</mat-icon>
            </div>
            <div class="kpi-info">
              <span class="label">{{ 'SALE.ADMIN.VISITS' | translate }}</span>
              <span class="value">{{ stats.visitsCount }}</span>
            </div>
          </mat-card>

          <mat-card class="kpi-card" data-cy="stat-days">
            <div class="kpi-icon orange">
              <mat-icon>schedule</mat-icon>
            </div>
            <div class="kpi-info">
              <span class="label">{{ 'SALE.ADMIN.DAYS_ONLINE' | translate }}</span>
              <span class="value">{{ stats.daysOnline }} j</span>
            </div>
          </mat-card>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button (click)="close()" data-cy="close-stats-btn">{{ 'COMMON.CLOSE' | translate }}</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .stats-dialog {
        padding: 0.5rem;
      }
      h2[mat-dialog-title] {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.25rem;
      }
      .listing-subtitle {
        color: #64748b;
        margin-bottom: 1.5rem;
        font-weight: 500;
      }
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 1rem;
        margin-bottom: 1rem;
      }
      .kpi-card {
        padding: 1rem;
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: 1rem;
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }
      .kpi-icon {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .kpi-icon.blue { background: #e0f2fe; color: #0284c7; }
      .kpi-icon.green { background: #dcfce7; color: #16a34a; }
      .kpi-icon.purple { background: #f3e8ff; color: #9333ea; }
      .kpi-icon.orange { background: #ffedd5; color: #ea580c; }
      .kpi-info {
        display: flex;
        flex-direction: column;
      }
      .kpi-info .label {
        font-size: 0.85rem;
        color: #64748b;
      }
      .kpi-info .value {
        font-size: 1.5rem;
        font-weight: 700;
        color: #0f172a;
      }
    `,
  ],
})
export class SaleStatsComponent {
  readonly stats: SaleListingStats;

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: SaleStatsDialogData,
    private readonly dialogRef: MatDialogRef<SaleStatsComponent>,
  ) {
    this.stats = data.stats ?? this.computeMockStats(data.listing);
  }

  private computeMockStats(listing: SaleListing): SaleListingStats {
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

  close(): void {
    this.dialogRef.close();
  }
}
