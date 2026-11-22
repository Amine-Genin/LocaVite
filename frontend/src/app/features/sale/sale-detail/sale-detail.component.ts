import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { VehicleHistoryEvent } from '../../vehicles/models/vehicle-history.model';
import { VehiclePhoto } from '../../vehicles/models/vehicle.model';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { PhotoGalleryComponent } from '../../vehicles/photo-gallery/photo-gallery.component';
import { SaleListing, SaleListingPage } from '../models/sale-listing.model';
import { SaleListingService } from '../services/sale-listing.service';
import { SaleCardComponent } from '../sale-card/sale-card.component';
import { ContactModalComponent } from './contact-modal/contact-modal.component';
import { HistoryTimelineComponent } from './history-timeline/history-timeline.component';
import { VisitModalComponent } from './visit-modal/visit-modal.component';
import { VehicleHistoryService } from '../../vehicles/services/vehicle-history.service';

@Component({
  selector: 'app-sale-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    TranslateModule,
    PhotoGalleryComponent,
    HistoryTimelineComponent,
    SaleCardComponent,
  ],
  templateUrl: './sale-detail.component.html',
  styleUrl: './sale-detail.component.scss',
})
export class SaleDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly saleListingService = inject(SaleListingService);
  private readonly vehicleService = inject(VehicleService);
  private readonly historyService = inject(VehicleHistoryService);
  private readonly dialog = inject(MatDialog);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);

  readonly listing = signal<SaleListing | null>(null);
  readonly photos = signal<VehiclePhoto[]>([]);
  readonly historyEvents = signal<VehicleHistoryEvent[]>([]);
  readonly similarListings = signal<SaleListing[]>([]);
  readonly estimatedPrice = signal<number | null>(null);
  readonly estimateCount = signal(0);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly historyLoading = signal(false);
  readonly historyError = signal(false);
  readonly photosError = signal(false);
  readonly similarError = signal(false);
  readonly estimateLoading = signal(false);
  readonly estimateVisible = signal(false);
  readonly isFavorite = signal(false);
  readonly copyStatus = signal<'idle' | 'copied' | 'error'>('idle');

  private routeSubscription?: Subscription;
  private detailSubscriptions = new Subscription();
  private readonly defaultTitle = 'LocaVite · Annonces de vente';

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.loadListing(Number.isSafeInteger(id) && id > 0 ? id : null);
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
    this.detailSubscriptions.unsubscribe();
    this.titleService.setTitle(this.defaultTitle);
    this.metaService.removeTag("name='description'");
    this.metaService.removeTag("property='og:title'");
    this.metaService.removeTag("property='og:description'");
    this.metaService.removeTag("property='og:image'");
    this.metaService.removeTag("property='og:type'");
  }

  toggleFavorite(): void {
    const current = this.listing();
    if (!current) return;
    this.isFavorite.set(this.vehicleService.toggleFavorite(current.vehicleId));
  }

  openContactModal(): void {
    const current = this.listing();
    if (!current) return;
    this.dialog.open(ContactModalComponent, {
      data: { listing: current },
      width: '560px',
      maxWidth: '95vw',
    });
  }

  openVisitModal(): void {
    const current = this.listing();
    if (!current) return;
    this.dialog.open(VisitModalComponent, {
      data: { listing: current },
      width: '560px',
      maxWidth: '95vw',
    });
  }

  shareOnWhatsApp(): void {
    const current = this.listing();
    if (!current || typeof window === 'undefined') return;
    const text = `${current.marque} ${current.modele} (${current.annee}) · ${window.location.href}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank', 'noopener,noreferrer');
  }

  copyLink(): void {
    if (typeof navigator === 'undefined' || !navigator.clipboard) {
      this.copyStatus.set('error');
      return;
    }

    void navigator.clipboard
      .writeText(window.location.href)
      .then(() => this.copyStatus.set('copied'))
      .catch(() => this.copyStatus.set('error'));
  }

  toggleEstimate(): void {
    this.estimateVisible.update((visible) => !visible);
  }

  private loadListing(id: number | null): void {
    this.detailSubscriptions.unsubscribe();
    this.detailSubscriptions = new Subscription();
    this.listing.set(null);
    this.photos.set([]);
    this.historyEvents.set([]);
    this.similarListings.set([]);
    this.estimatedPrice.set(null);
    this.estimateCount.set(0);
    this.estimateVisible.set(false);
    this.error.set(id === null);
    this.loading.set(id !== null);
    this.historyLoading.set(false);
    this.historyError.set(false);
    this.photosError.set(false);
    this.similarError.set(false);

    if (id === null) return;

    this.detailSubscriptions.add(
      this.saleListingService.getById(id).subscribe({
        next: (listing) => {
          this.listing.set(listing);
          this.isFavorite.set(this.vehicleService.isFavorite(listing.vehicleId));
          this.loading.set(false);
          this.updateSeo(listing);
          this.loadPhotos(listing);
          this.loadHistory(listing);
          this.loadSimilar(listing);
        },
        error: () => {
          this.loading.set(false);
          this.error.set(true);
        },
      }),
    );
  }

  private loadPhotos(listing: SaleListing): void {
    this.photos.set(
      listing.photoUrl
        ? [{ id: 0, vehicleId: listing.vehicleId, url: listing.photoUrl, isPrimary: true }]
        : [],
    );
    this.detailSubscriptions.add(
      this.vehicleService.getPhotos(listing.vehicleId).subscribe({
        next: (photos) => {
          if (photos.length > 0) this.photos.set(photos);
        },
        error: () => this.photosError.set(true),
      }),
    );
  }

  private loadHistory(listing: SaleListing): void {
    if (listing.etat !== 'USED') return;
    this.historyLoading.set(true);
    this.detailSubscriptions.add(
      this.historyService.getHistoryForListing(listing.id).subscribe({
        next: (events) => {
          this.historyEvents.set(events);
          this.historyLoading.set(false);
        },
        error: () => {
          this.historyLoading.set(false);
          this.historyError.set(true);
        },
      }),
    );
  }

  private loadSimilar(listing: SaleListing): void {
    this.estimateLoading.set(true);
    this.detailSubscriptions.add(
      this.saleListingService
        .search({
          marque: listing.marque,
          modele: listing.modele,
          anneeMin: Math.max(1900, listing.annee - 2),
          anneeMax: listing.annee + 2,
          page: 0,
          size: 100,
          sort: 'datePublication,desc',
        })
        .subscribe({
          next: (page: SaleListingPage) => {
            const comparableListings = page.content.filter(
              (item) => item.id !== listing.id && item.prixVente > 0,
            );
            this.similarListings.set(comparableListings.slice(0, 3));
            this.estimatedPrice.set(this.median(comparableListings.map((item) => item.prixVente)));
            this.estimateCount.set(comparableListings.length);
            this.estimateLoading.set(false);
          },
          error: () => {
            this.similarError.set(true);
            this.estimateLoading.set(false);
          },
        }),
    );
  }

  private median(prices: number[]): number | null {
    if (prices.length === 0) return null;
    const ordered = [...prices].sort((a, b) => a - b);
    const middle = Math.floor(ordered.length / 2);
    return ordered.length % 2 === 0
      ? Math.round((ordered[middle - 1] + ordered[middle]) / 2)
      : ordered[middle];
  }

  private updateSeo(listing: SaleListing): void {
    const title = `${listing.marque} ${listing.modele} ${listing.annee} | LocaVite`;
    const description =
      listing.description ||
      `${listing.marque} ${listing.modele} ${listing.annee} à vendre à ${listing.ville}.`;

    this.titleService.setTitle(title);
    this.metaService.updateTag({ name: 'description', content: description });
    this.metaService.updateTag({ property: 'og:title', content: title });
    this.metaService.updateTag({ property: 'og:description', content: description });
    this.metaService.updateTag({
      property: 'og:image',
      content: listing.photoUrl || 'https://locavite.ma/assets/images/auth-banner.jpg',
    });
    this.metaService.updateTag({ property: 'og:type', content: 'product' });
  }
}
