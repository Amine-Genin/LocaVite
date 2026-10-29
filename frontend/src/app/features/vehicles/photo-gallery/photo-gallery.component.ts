import { Component, HostListener, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { VehiclePhoto } from '../models/vehicle.model';
import { DEFAULT_VEHICLE_PHOTO } from '../vehicle-card/vehicle-card.component';

@Component({
  selector: 'app-photo-gallery',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './photo-gallery.component.html',
  styleUrl: './photo-gallery.component.scss',
})
export class PhotoGalleryComponent implements OnChanges {
  @Input() photos: (VehiclePhoto | string)[] = [];
  @Input() alt = 'Photo véhicule';

  currentIndex = 0;
  private touchStartX = 0;
  private touchStartY = 0;

  readonly defaultPhoto = DEFAULT_VEHICLE_PHOTO;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['photos']) {
      this.currentIndex = 0;
    }
  }

  get normalizedPhotos(): string[] {
    if (!this.photos || this.photos.length === 0) {
      return [this.defaultPhoto];
    }
    return this.photos.map((p) => {
      if (typeof p === 'string') {
        return p || this.defaultPhoto;
      }
      return p?.url || p?.thumbnailUrl || this.defaultPhoto;
    });
  }

  get currentPhotoUrl(): string {
    const list = this.normalizedPhotos;
    return list[this.currentIndex] ?? list[0] ?? this.defaultPhoto;
  }

  get totalPhotos(): number {
    return this.normalizedPhotos.length;
  }

  next(): void {
    if (this.totalPhotos <= 1) return;
    this.currentIndex = (this.currentIndex + 1) % this.totalPhotos;
  }

  prev(): void {
    if (this.totalPhotos <= 1) return;
    this.currentIndex = (this.currentIndex - 1 + this.totalPhotos) % this.totalPhotos;
  }

  select(index: number): void {
    if (index >= 0 && index < this.totalPhotos) {
      this.currentIndex = index;
    }
  }

  onTouchStart(event: TouchEvent): void {
    if (event.touches.length > 0) {
      this.touchStartX = event.touches[0].clientX;
      this.touchStartY = event.touches[0].clientY;
    }
  }

  onTouchEnd(event: TouchEvent): void {
    if (event.changedTouches.length === 0) return;
    const deltaX = event.changedTouches[0].clientX - this.touchStartX;
    const deltaY = event.changedTouches[0].clientY - this.touchStartY;

    // Detect horizontal swipe if deltaX is sufficiently larger than vertical movement
    if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX < 0) {
        this.next();
      } else {
        this.prev();
      }
    }
  }

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'ArrowLeft') {
      this.prev();
    } else if (event.key === 'ArrowRight') {
      this.next();
    }
  }

  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target && target.src !== this.defaultPhoto) {
      target.src = this.defaultPhoto;
    }
  }
}
