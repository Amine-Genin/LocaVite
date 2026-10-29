import { Component, EventEmitter, Input, OnChanges, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Vehicle } from '../models/vehicle.model';

export const DEFAULT_VEHICLE_PHOTO =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='640' height='400' viewBox='0 0 640 400'><rect width='640' height='400' fill='%23e9f0fa'/><path d='M165 255l35-70c8-16 24-25 42-25h156c18 0 34 9 42 25l35 70h20c14 0 25 11 25 25v30c0 8-7 15-15 15h-25c-8 0-15-7-15-15v-10H175v10c0 8-7 15-15 15h-25c-8 0-15-7-15-15v-30c0-14 11-25 25-25h20zm32 0h246l-24-48c-4-8-12-13-21-13H242c-9 0-17 5-21 13l-24 48zm-7 32a18 18 0 1 0 0 36 18 18 0 0 0 0-36zm260 0a18 18 0 1 0 0 36 18 18 0 0 0 0-36z' fill='%23142e45' opacity='0.28'/></svg>";

const FALLBACK_FR_TRANSLATIONS = {
  COMMON: {
    CURRENCY_MAD: 'DH',
  },
  VEHICLES: {
    PER_DAY: '/ jour',
    YEAR: 'Année',
    CITY: 'Ville',
    CATEGORY: 'Catégorie',
    DETAILS: 'Détails',
    ADD_FAVORITE: 'Ajouter aux favoris',
    REMOVE_FAVORITE: 'Retirer des favoris',
  },
};

@Component({
  selector: 'app-vehicle-card',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, RouterLink, TranslateModule],
  templateUrl: './vehicle-card.component.html',
  styleUrl: './vehicle-card.component.scss',
})
export class VehicleCardComponent implements OnChanges {
  private readonly translate = inject(TranslateService, { optional: true });

  @Input({ required: true }) vehicle!: Vehicle;
  @Input() isFavorite?: boolean;

  @Output() favoriteToggle = new EventEmitter<Vehicle>();
  @Output() detailsClick = new EventEmitter<Vehicle>();

  imageBroken = false;
  localFavorite = false;

  constructor() {
    this.ensureFallbackTranslations();
  }

  ngOnChanges(): void {
    this.imageBroken = false;
    this.localFavorite = Boolean(this.isFavorite ?? this.vehicle?.isFavorite);
  }

  get photoSrc(): string {
    if (this.imageBroken || !this.vehicle?.photoUrl) {
      return DEFAULT_VEHICLE_PHOTO;
    }
    return this.vehicle.photoUrl;
  }

  get favoriteState(): boolean {
    if (this.isFavorite !== undefined) {
      return this.isFavorite;
    }
    return this.localFavorite || Boolean(this.vehicle?.isFavorite);
  }

  onImageError(): void {
    this.imageBroken = true;
  }

  onToggleFavorite(event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.localFavorite = !this.favoriteState;
    if (this.vehicle) {
      this.vehicle.isFavorite = this.localFavorite;
      this.favoriteToggle.emit(this.vehicle);
    }
  }

  onDetailsClick(): void {
    if (this.vehicle) {
      this.detailsClick.emit(this.vehicle);
    }
  }

  private ensureFallbackTranslations(): void {
    if (!this.translate) return;
    if (this.translate.instant('VEHICLES.DETAILS') === 'VEHICLES.DETAILS') {
      this.translate.setTranslation('fr', FALLBACK_FR_TRANSLATIONS, true);
      if (!this.translate.currentLang) {
        this.translate.use('fr');
      }
    }
  }
}
