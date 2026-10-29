import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { Subscription, catchError, of, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AvailabilityResponse, Vehicle } from '../models/vehicle.model';
import { VehicleService } from '../services/vehicle.service';
import { PhotoGalleryComponent } from '../photo-gallery/photo-gallery.component';
import { AvailabilityCalendarComponent } from '../availability-calendar/availability-calendar.component';
import { VehicleCardComponent } from '../vehicle-card/vehicle-card.component';
import { VehicleContactDialogComponent } from './contact-dialog/vehicle-contact-dialog.component';
import { AuthHeaderComponent } from '../../../shared/components/auth-header/auth-header.component';
import { AuthFooterComponent } from '../../../shared/components/auth-footer/auth-footer.component';
import { setupFallbackTranslations } from '../vehicles.constants';

@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatTabsModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    TranslateModule,
    PhotoGalleryComponent,
    AvailabilityCalendarComponent,
    VehicleCardComponent,
    AuthHeaderComponent,
    AuthFooterComponent,
  ],
  templateUrl: './vehicle-detail.component.html',
  styleUrl: './vehicle-detail.component.scss',
})
export class VehicleDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vehicleService = inject(VehicleService);
  private readonly dialog = inject(MatDialog);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly translate = inject(TranslateService, { optional: true });

  vehicle = signal<Vehicle | null>(null);
  availability = signal<AvailabilityResponse | undefined>(undefined);
  similarVehicles = signal<Vehicle[]>([]);
  loading = signal(true);
  error = signal(false);
  isFavorite = signal(false);

  private routeSub?: Subscription;
  private readonly defaultTitle = 'LocaVite · Location & Vente de Véhicules au Maroc';

  constructor() {
    setupFallbackTranslations(this.translate);
  }

  ngOnInit(): void {
    this.routeSub = this.route.paramMap
      .pipe(
        switchMap((params) => {
          const idStr = params.get('id');
          const id = idStr ? Number(idStr) : Number.NaN;
          if (Number.isNaN(id)) {
            this.error.set(true);
            this.loading.set(false);
            return of(null);
          }

          this.loading.set(true);
          this.error.set(false);

          return this.vehicleService.getById(id).pipe(
            catchError(() => {
              this.error.set(true);
              this.loading.set(false);
              return of(null);
            }),
          );
        }),
      )
      .subscribe((veh) => {
        if (!veh) return;
        this.setVehicleData(veh);
      });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.titleService.setTitle(this.defaultTitle);
    this.metaService.removeTag("name='description'");
    this.metaService.removeTag("property='og:title'");
    this.metaService.removeTag("property='og:description'");
    this.metaService.removeTag("property='og:image'");
  }

  setVehicleData(veh: Vehicle): void {
    this.vehicle.set(veh);
    this.isFavorite.set(this.vehicleService.isFavorite(veh.id));
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    this.loading.set(false);
    this.error.set(false);

    // Apply dynamic SEO tags
    this.updateSeoMeta(veh);

    // Fetch availability
    const today = new Date();
    const futureDate = new Date();
    futureDate.setMonth(futureDate.getMonth() + 2);

    const fromIso = today.toISOString().slice(0, 10);
    const toIso = futureDate.toISOString().slice(0, 10);

    this.vehicleService
      .getAvailability(veh.id, fromIso, toIso)
      .pipe(catchError(() => of({ available: [], unavailable: [] })))
      .subscribe((res) => {
        this.availability.set(res);
      });

    // Fetch 3 similar vehicles in same category
    this.vehicleService
      .search({ categorie: veh.categorie, size: 4 })
      .pipe(catchError(() => of({ content: [] })))
      .subscribe((res) => {
        const others = (res?.content ?? []).filter((v) => v.id !== veh.id).slice(0, 3);
        this.similarVehicles.set(others);
      });
  }

  onToggleFavorite(): void {
    const current = this.vehicle();
    if (!current) return;
    const added = this.vehicleService.toggleFavorite(current.id);
    this.isFavorite.set(added);
  }

  onReserve(): void {
    const current = this.vehicle();
    if (current) {
      void this.router.navigate(['/reservation', current.id]);
    }
  }

  openContactDialog(): void {
    const current = this.vehicle();
    if (!current) return;

    this.dialog.open(VehicleContactDialogComponent, {
      data: { vehicle: current },
      width: '560px',
      maxWidth: '95vw',
    });
  }

  onSimilarFavoriteToggle(item: Vehicle): void {
    this.vehicleService.toggleFavorite(item.id);
  }

  private updateSeoMeta(veh: Vehicle): void {
    const titleText = `${veh.marque} ${veh.modele} (${veh.annee}) | LocaVite Maroc`;
    const descText =
      veh.description ||
      `Louez ou achetez ${veh.marque} ${veh.modele} ${veh.annee} à ${veh.ville} au meilleur prix sur LocaVite.`;
    const photo = veh.photoUrl || 'https://locavite.ma/assets/images/auth-banner.jpg';

    this.titleService.setTitle(titleText);

    this.metaService.updateTag({ name: 'description', content: descText });
    this.metaService.updateTag({ property: 'og:title', content: titleText });
    this.metaService.updateTag({ property: 'og:description', content: descText });
    this.metaService.updateTag({ property: 'og:image', content: photo });
    this.metaService.updateTag({ property: 'og:type', content: 'website' });
    this.metaService.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
  }
}
