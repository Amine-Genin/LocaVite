import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { catchError, finalize, of, switchMap } from 'rxjs';

import {
  FuelType,
  TransmissionType,
  Vehicle,
  UploadedPhotoItem,
} from '../../vehicles/models/vehicle.model';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { PhotoUploaderComponent } from '../../vehicles/photo-uploader/photo-uploader.component';
import { MOROCCAN_CITIES, generateYearsList } from '../../vehicles/vehicles.constants';
import {
  SaleListingCreateRequest,
  SalePriceEstimate,
  SaleCondition,
  SaleStatus,
} from '../models/sale-listing.model';
import { SaleListingService } from '../services/sale-listing.service';

@Component({
  selector: 'app-sale-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    TranslateModule,
    PhotoUploaderComponent,
  ],
  templateUrl: './sale-form.component.html',
  styleUrl: './sale-form.component.scss',
})
export class SaleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly vehicleService = inject(VehicleService);
  private readonly saleListingService = inject(SaleListingService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly vehicles = signal<Vehicle[]>([]);
  readonly brands = signal<{ id: number; name: string }[]>([]);
  readonly categories = signal<{ id: number; name: string }[]>([]);
  readonly cities = MOROCCAN_CITIES;
  readonly years = generateYearsList();
  readonly estimate = signal<SalePriceEstimate | null>(null);
  readonly estimating = signal(false);
  readonly submitting = signal(false);
  readonly preview = signal(false);
  readonly errorMessage = signal('');
  photos: UploadedPhotoItem[] = [];

  readonly vehicleForm = this.fb.group({
    mode: ['EXISTING' as 'EXISTING' | 'NEW', Validators.required],
    vehicleId: [null as number | null],
    brandId: [null as number | null],
    modele: [''],
    annee: [new Date().getFullYear()],
    categoryId: [null as number | null],
    ville: ['Casablanca'],
    prixJour: [350],
    prixVenteVehicle: [null as number | null],
    carburant: ['ESSENCE'],
    transmission: ['MANUELLE'],
    places: [5],
    matricule: [''],
    vehicleDescription: [''],
  });

  readonly conditionForm = this.fb.group({
    etat: ['USED' as SaleCondition, Validators.required],
  });

  readonly priceForm = this.fb.group({
    prixVente: [null as number | null, [Validators.required, Validators.min(1)]],
    prixNego: [true],
  });

  readonly detailsForm = this.fb.group({
    numeroChassis: [''],
    kilometrage: [null as number | null],
    dateMiseEnCirculation: [null as Date | null],
    nombreProprietaires: [1],
  });

  readonly descriptionForm = this.fb.group({
    titre: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.required],
  });

  readonly publicationForm = this.fb.group({
    statut: ['DRAFT' as SaleStatus, Validators.required],
  });

  ngOnInit(): void {
    this.loadMetadata();
    this.loadVehicles();
    this.vehicleForm.controls.mode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateVehicleValidators());
    this.conditionForm.controls.etat.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((etat) => this.updateConditionValidators(etat));
    this.updateVehicleValidators();
    this.updateConditionValidators('USED');
  }

  private loadVehicles(): void {
    this.vehicleService
      .search({ page: 0, size: 100 })
      .pipe(
        catchError(() => of({ content: [] })),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => this.vehicles.set(page.content));
  }

  private loadMetadata(): void {
    this.vehicleService
      .getBrands()
      .pipe(
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((brands) => this.brands.set(brands));
    this.vehicleService
      .getCategories()
      .pipe(
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((categories) => this.categories.set(categories));
  }

  private updateVehicleValidators(): void {
    const isNew = this.vehicleForm.controls.mode.value === 'NEW';
    const controls = this.vehicleForm.controls;
    if (isNew) {
      controls.brandId.setValidators([Validators.required]);
      controls.modele.setValidators([Validators.required, Validators.maxLength(100)]);
      controls.annee.setValidators([Validators.required, Validators.min(1990)]);
      controls.categoryId.setValidators([Validators.required]);
      controls.prixJour.setValidators([Validators.required, Validators.min(1)]);
      controls.prixVenteVehicle.setValidators([Validators.required, Validators.min(1)]);
      controls.matricule.setValidators([Validators.required]);
      controls.vehicleDescription.setValidators([Validators.required]);
      controls.vehicleId.clearValidators();
    } else {
      controls.vehicleId.setValidators([Validators.required]);
      for (const control of [
        controls.brandId,
        controls.modele,
        controls.annee,
        controls.categoryId,
        controls.prixJour,
        controls.prixVenteVehicle,
        controls.matricule,
        controls.vehicleDescription,
      ]) {
        control.clearValidators();
      }
    }
    Object.values(controls).forEach((control) => control.updateValueAndValidity());
  }

  private updateConditionValidators(etat: SaleCondition | null): void {
    const kilometers = this.detailsForm.controls.kilometrage;
    const date = this.detailsForm.controls.dateMiseEnCirculation;
    const owners = this.detailsForm.controls.nombreProprietaires;

    if (etat === 'NEW') {
      kilometers.setValue(0);
      kilometers.disable();
      date.setValue(null);
      date.disable();
      date.clearValidators();
      owners.setValue(0);
      owners.disable();
    } else {
      kilometers.enable();
      kilometers.setValidators([Validators.required, Validators.min(1)]);
      date.enable();
      date.setValidators([Validators.required]);
      owners.enable();
      owners.setValidators([Validators.required, Validators.min(1)]);
    }
    kilometers.updateValueAndValidity();
    date.updateValueAndValidity();
    owners.updateValueAndValidity();
  }

  onVehicleChanged(vehicleId: number): void {
    const vehicle = this.vehicles().find((item) => item.id === vehicleId);
    if (vehicle) {
      if (!this.descriptionForm.controls.titre.value) {
        this.descriptionForm.controls.titre.setValue(`${vehicle.marque} ${vehicle.modele}`);
      }
      if (!this.priceForm.controls.prixVente.value && vehicle.prixVente) {
        this.priceForm.controls.prixVente.setValue(vehicle.prixVente);
      }
    }
  }

  estimatePrice(): void {
    const mode = this.vehicleForm.controls.mode.value;
    let marque = '';
    let modele = '';
    let categorie = '';
    let annee = new Date().getFullYear();
    let carburant = '';
    let ville = '';

    if (mode === 'EXISTING') {
      const vehicle = this.vehicles().find(
        (item) => item.id === this.vehicleForm.controls.vehicleId.value,
      );
      if (vehicle) {
        marque = vehicle.marque;
        modele = vehicle.modele;
        categorie = vehicle.categorie;
        annee = vehicle.annee;
        carburant = vehicle.carburant || '';
        ville = vehicle.ville;
      }
    } else {
      const brand = this.brands().find(
        (item) => item.id === this.vehicleForm.controls.brandId.value,
      );
      const category = this.categories().find(
        (item) => item.id === this.vehicleForm.controls.categoryId.value,
      );
      marque = brand?.name || '';
      modele = this.vehicleForm.controls.modele.value;
      categorie = category?.name || '';
      annee = this.vehicleForm.controls.annee.value;
      carburant = this.vehicleForm.controls.carburant.value;
      ville = this.vehicleForm.controls.ville.value;
    }

    this.estimating.set(true);
    this.saleListingService
      .estimatePrice({
        marque: marque || undefined,
        modele: modele || undefined,
        categorie: categorie || undefined,
        etat: this.conditionForm.controls.etat.value,
        anneeMin: Math.max(1990, annee - 2),
        anneeMax: annee + 2,
        carburant: carburant || undefined,
        ville: ville || undefined,
      })
      .pipe(finalize(() => this.estimating.set(false)))
      .subscribe({
        next: (estimate) => this.estimate.set(estimate),
        error: () => this.errorMessage.set('Impossible de calculer une estimation pour le moment.'),
      });
  }

  onPhotosChange(photos: UploadedPhotoItem[]): void {
    this.photos = photos;
  }

  togglePreview(): void {
    this.preview.update((visible) => !visible);
  }

  submit(statut: SaleStatus = this.publicationForm.controls.statut.value): void {
    const forms = [
      this.vehicleForm,
      this.conditionForm,
      this.priceForm,
      this.detailsForm,
      this.descriptionForm,
    ];
    if (forms.some((form) => form.invalid) || this.submitting()) {
      forms.forEach((form) => form.markAllAsTouched());
      this.errorMessage.set('Veuillez corriger les champs obligatoires.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    const selectedVehicleId = this.vehicleForm.controls.vehicleId.value;
    const createVehicle$ =
      this.vehicleForm.controls.mode.value === 'NEW'
        ? this.vehicleService.create({
            brandId: Number(this.vehicleForm.controls.brandId.value),
            modele: this.vehicleForm.controls.modele.value.trim(),
            annee: Number(this.vehicleForm.controls.annee.value),
            categoryId: Number(this.vehicleForm.controls.categoryId.value),
            ville: this.vehicleForm.controls.ville.value,
            prixJour: Number(this.vehicleForm.controls.prixJour.value),
            prixVente: Number(this.vehicleForm.controls.prixVenteVehicle.value),
            carburant: this.vehicleForm.controls.carburant.value as FuelType,
            transmission: this.vehicleForm.controls.transmission.value as TransmissionType,
            places: Number(this.vehicleForm.controls.places.value),
            description: this.vehicleForm.controls.vehicleDescription.value.trim(),
            matricule: this.vehicleForm.controls.matricule.value.trim(),
            statut: 'PUBLISHED',
          })
        : of(this.vehicles().find((item) => item.id === selectedVehicleId) as Vehicle);

    createVehicle$
      .pipe(
        switchMap((vehicle) => {
          if (!vehicle?.id) throw new Error('Véhicule introuvable');
          const rawDate = this.detailsForm.controls.dateMiseEnCirculation.value;
          const request: SaleListingCreateRequest = {
            vehicleId: vehicle.id,
            titre: this.descriptionForm.controls.titre.value.trim(),
            description: this.descriptionForm.controls.description.value.trim(),
            prixVente: Number(this.priceForm.controls.prixVente.value),
            prixNego: this.priceForm.controls.prixNego.value,
            etat: this.conditionForm.controls.etat.value,
            kilometrage: this.detailsForm.controls.kilometrage.value ?? 0,
            premierProprietaire: this.detailsForm.controls.nombreProprietaires.value <= 1,
            dateMiseEnCirculation: rawDate
              ? `${rawDate.getFullYear()}-${String(rawDate.getMonth() + 1).padStart(2, '0')}-${String(rawDate.getDate()).padStart(2, '0')}`
              : null,
            numeroChassis: this.detailsForm.controls.numeroChassis.value.trim() || null,
            nombreProprietaires: this.detailsForm.controls.nombreProprietaires.value,
            statut,
          };
          return this.saleListingService.create(request).pipe(
            switchMap((listing) => {
              const files = this.photos
                .map((photo) => photo.file)
                .filter((file): file is File => !!file);
              if (files.length === 0) return of(listing);
              return this.vehicleService.uploadPhotos(vehicle.id, files).pipe(
                switchMap((uploaded) => {
                  const primaryIndex = this.photos.findIndex((photo) => photo.isPrimary);
                  if (primaryIndex >= 0 && uploaded[primaryIndex]) {
                    return this.vehicleService
                      .setPrimaryPhoto(vehicle.id, uploaded[primaryIndex].id)
                      .pipe(switchMap(() => of(listing)));
                  }
                  return of(listing);
                }),
                catchError(() => of(listing)),
              );
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            statut === 'PUBLISHED' ? 'Annonce publiée avec succès' : 'Brouillon enregistré',
            'Fermer',
            { duration: 3000 },
          );
          void this.router.navigate(['/admin/sale']);
        },
        error: (error) => {
          this.errorMessage.set(
            error?.error?.message || error?.message || 'Erreur lors de la création',
          );
        },
      });
  }
}
