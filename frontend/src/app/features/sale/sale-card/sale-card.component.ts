import { Component, Input, OnChanges, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';

import { VehicleService } from '../../vehicles/services/vehicle.service';
import { DEFAULT_VEHICLE_PHOTO } from '../../vehicles/vehicle-card/vehicle-card.component';
import { SaleListing } from '../models/sale-listing.model';

@Component({
  selector: 'app-sale-card',
  standalone: true,
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatCardModule, TranslateModule],
  templateUrl: './sale-card.component.html',
  styleUrl: './sale-card.component.scss',
})
export class SaleCardComponent implements OnChanges {
  private readonly vehicleService = inject(VehicleService);

  @Input({ required: true }) listing!: SaleListing;

  favorite = false;
  imageBroken = false;

  ngOnChanges(): void {
    this.favorite = this.vehicleService.isFavorite(this.listing.vehicleId);
    this.imageBroken = false;
  }

  get photoSrc(): string {
    return this.imageBroken || !this.listing.photoUrl
      ? DEFAULT_VEHICLE_PHOTO
      : this.listing.photoUrl;
  }

  onImageError(): void {
    this.imageBroken = true;
  }

  toggleFavorite(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favorite = this.vehicleService.toggleFavorite(this.listing.vehicleId);
  }
}
