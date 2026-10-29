import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import {
  CatalogEntry,
  UploadedPhotoItem,
  VehicleCreateRequest,
  VehiclePhoto,
} from '../models/vehicle.model';
import { VehicleService } from '../services/vehicle.service';
import { PhotoUploaderComponent } from '../photo-uploader/photo-uploader.component';
import {
  MOROCCAN_CITIES,
  generateYearsList,
  setupFallbackTranslations,
} from '../vehicles.constants';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatRadioModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
    PhotoUploaderComponent,
  ],
  templateUrl: './vehicle-form.component.html',
  styleUrl: './vehicle-form.component.scss',
})
export class VehicleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly vehicleService = inject(VehicleService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService, { optional: true });

  readonly cities = MOROCCAN_CITIES;
  readonly years = generateYearsList();

  brands: CatalogEntry[] = [];
  categories: CatalogEntry[] = [];

  brandSearchControl = new FormControl<string>('');
  filteredBrands$!: Observable<CatalogEntry[]>;

  photos: UploadedPhotoItem[] = [];

  isSubmitting = false;
  isLoadingMetadata = false;
  errorMessage: string | null = null;

  form: FormGroup = this.fb.group({
    brandId: [null as number | null, [Validators.required]],
    modele: ['', [Validators.required, Validators.maxLength(100)]],
    annee: [
      new Date().getFullYear(),
      [Validators.required, Validators.min(1990), Validators.max(2026)],
    ],
    categoryId: [null as number | null, [Validators.required]],
    carburant: ['ESSENCE', [Validators.required]],
    transmission: ['MANUELLE', [Validators.required]],
    places: [5, [Validators.required, Validators.min(2), Validators.max(9)]],
    matricule: ['', [Validators.required]],
    description: ['', [Validators.required]],
    prixJour: [null as number | null, [Validators.required, Validators.min(1)]],
    prixVente: [null as number | null, [Validators.required, Validators.min(1)]],
    ville: ['Casablanca', [Validators.required]],
  });

  constructor() {
    setupFallbackTranslations(this.translate);
  }

  ngOnInit(): void {
    this.setupBrandFiltering();
    this.loadMetadata();
  }

  private setupBrandFiltering(): void {
    this.filteredBrands$ = this.brandSearchControl.valueChanges.pipe(
      startWith(''),
      map((value) => {
        const query = typeof value === 'string' ? value.toLowerCase() : '';
        if (!query) {
          return this.brands;
        }
        return this.brands.filter((brand) => brand.name.toLowerCase().includes(query));
      }),
    );

    // Keep form brandId in sync if user types full brand name or clears
    this.brandSearchControl.valueChanges.subscribe((val) => {
      if (typeof val === 'string') {
        const matchingBrand = this.brands.find(
          (b) => b.name.toLowerCase() === val.trim().toLowerCase(),
        );
        if (matchingBrand) {
          this.form.get('brandId')?.setValue(matchingBrand.id);
        } else if (!val.trim()) {
          this.form.get('brandId')?.setValue(null);
        }
      }
    });
  }

  loadMetadata(): void {
    this.isLoadingMetadata = true;

    this.vehicleService
      .getBrands()
      .pipe(
        catchError(() =>
          of([
            { id: 1, name: 'Renault' },
            { id: 2, name: 'Dacia' },
            { id: 3, name: 'Peugeot' },
            { id: 4, name: 'Volkswagen' },
            { id: 5, name: 'Toyota' },
            { id: 6, name: 'Hyundai' },
            { id: 7, name: 'Mercedes-Benz' },
            { id: 8, name: 'BMW' },
          ]),
        ),
      )
      .subscribe((brands) => {
        this.brands = brands;
        this.brandSearchControl.updateValueAndValidity({ onlySelf: true, emitEvent: true });
      });

    this.vehicleService
      .getCategories()
      .pipe(
        catchError(() =>
          of([
            { id: 1, name: 'Économique' },
            { id: 2, name: 'Berline' },
            { id: 3, name: 'SUV' },
            { id: 4, name: 'Compacte' },
            { id: 5, name: 'Luxe' },
            { id: 6, name: 'Utilitaire' },
          ]),
        ),
      )
      .subscribe((categories) => {
        this.categories = categories;
        this.isLoadingMetadata = false;
      });
  }

  onBrandSelected(event: MatAutocompleteSelectedEvent): void {
    const selectedBrand: CatalogEntry = event.option.value;
    this.form.get('brandId')?.setValue(selectedBrand.id);
    this.form.get('brandId')?.markAsDirty();
    this.form.get('brandId')?.markAsTouched();
    this.brandSearchControl.setValue(selectedBrand.name, { emitEvent: false });
  }

  displayBrand(brand: CatalogEntry | string): string {
    if (!brand) return '';
    return typeof brand === 'string' ? brand : brand.name;
  }

  onPhotosChange(updatedPhotos: UploadedPhotoItem[]): void {
    this.photos = updatedPhotos;
  }

  onSubmit(statut: 'DRAFT' | 'PUBLISHED'): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    const raw = this.form.getRawValue();
    const request: VehicleCreateRequest = {
      brandId: Number(raw.brandId),
      modele: raw.modele.trim(),
      annee: Number(raw.annee),
      categoryId: Number(raw.categoryId),
      ville: raw.ville,
      prixJour: Number(raw.prixJour),
      prixVente: Number(raw.prixVente),
      carburant: raw.carburant,
      transmission: raw.transmission,
      places: Number(raw.places),
      description: raw.description.trim(),
      matricule: raw.matricule.trim(),
      statut,
    };

    this.vehicleService
      .create(request)
      .pipe(
        switchMap((createdVehicle) => {
          const filesToUpload = this.photos.map((p) => p.file).filter((f): f is File => !!f);

          if (filesToUpload.length === 0) {
            return of({ vehicle: createdVehicle, photos: [] as VehiclePhoto[] });
          }

          return this.vehicleService.uploadPhotos(createdVehicle.id, filesToUpload).pipe(
            switchMap((uploadedPhotos) => {
              const primaryIdx = this.photos.findIndex((p) => p.isPrimary);
              if (primaryIdx > 0 && uploadedPhotos[primaryIdx]) {
                return this.vehicleService
                  .setPrimaryPhoto(createdVehicle.id, uploadedPhotos[primaryIdx].id)
                  .pipe(
                    map(() => ({
                      vehicle: createdVehicle,
                      photos: uploadedPhotos,
                    })),
                    catchError(() =>
                      of({
                        vehicle: createdVehicle,
                        photos: uploadedPhotos,
                      }),
                    ),
                  );
              }
              return of({ vehicle: createdVehicle, photos: uploadedPhotos });
            }),
            catchError((uploadErr) => {
              console.error('Error uploading photos', uploadErr);
              return of({
                vehicle: createdVehicle,
                photos: [] as VehiclePhoto[],
              });
            }),
          );
        }),
      )
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          const msg =
            statut === 'PUBLISHED'
              ? 'Véhicule publié avec succès'
              : 'Brouillon enregistré avec succès';
          this.snackBar.open(msg, 'Fermer', { duration: 3000 });
          this.router.navigate(['/admin/vehicles']);
        },
        error: (err) => {
          this.isSubmitting = false;
          this.errorMessage =
            err?.error?.message ||
            err?.message ||
            'Une erreur est survenue lors de la création du véhicule.';
          this.snackBar.open(this.errorMessage!, 'Fermer', { duration: 5000 });
        },
      });
  }
}
