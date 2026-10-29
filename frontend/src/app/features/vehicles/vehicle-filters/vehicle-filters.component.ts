import { Component, EventEmitter, OnDestroy, Output, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { TranslateModule } from '@ngx-translate/core';
import { combineLatest, debounceTime, distinctUntilChanged, map, Subscription } from 'rxjs';

import { FuelType, TransmissionType, VehicleFilter } from '../models/vehicle.model';
import { VehicleService } from '../services/vehicle.service';

interface FilterFormValue {
  ville: string;
  dateDebut: Date | null;
  dateFin: Date | null;
  categorie: string;
  marque: string;
  carburant: FuelType | '';
  transmission: TransmissionType | '';
  prixMin: number | null;
  prixMax: number | null;
  sort: string;
}

@Component({
  selector: 'app-vehicle-filters',
  standalone: true,
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    TranslateModule,
  ],
  templateUrl: './vehicle-filters.component.html',
  styleUrl: './vehicle-filters.component.scss',
})
export class VehicleFiltersComponent implements OnDestroy {
  @Output() readonly filtersChange = new EventEmitter<Partial<VehicleFilter>>();

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vehicleService = inject(VehicleService);
  private readonly subscription: Subscription;
  private lastSynced: Partial<VehicleFilter>;

  readonly brands$ = this.vehicleService.getBrands();
  readonly categories$ = this.vehicleService.getCategories();
  readonly fuels: FuelType[] = ['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE'];
  readonly transmissions: TransmissionType[] = ['MANUELLE', 'AUTOMATIQUE'];
  readonly sortOptions = ['prixJour,asc', 'prixJour,desc', 'annee,desc', 'popularite,desc'];
  readonly form = new FormGroup({
    ville: new FormControl('', { nonNullable: true }),
    dateDebut: new FormControl<Date | null>(null),
    dateFin: new FormControl<Date | null>(null),
    categorie: new FormControl('', { nonNullable: true }),
    marque: new FormControl('', { nonNullable: true }),
    carburant: new FormControl<FuelType | ''>('', { nonNullable: true }),
    transmission: new FormControl<TransmissionType | ''>('', { nonNullable: true }),
    prixMin: new FormControl<number | null>(null),
    prixMax: new FormControl<number | null>(null),
    sort: new FormControl('', { nonNullable: true }),
  });

  drawerOpen = false;

  constructor() {
    this.lastSynced = this.fromQueryParams(this.route.snapshot.queryParams);
    this.form.patchValue(this.toFormValue(this.lastSynced), { emitEvent: false });

    this.subscription = new Subscription();
    this.subscription.add(this.route.queryParams.pipe(
      map((params) => this.fromQueryParams(params)),
      distinctUntilChanged((a, b) => this.sameFilters(a, b)),
    ).subscribe((routeFilters) => {
      if (this.sameFilters(routeFilters, this.lastSynced)) return;
      this.lastSynced = routeFilters;
      this.form.patchValue(this.toFormValue(routeFilters), { emitEvent: false });
      this.filtersChange.emit(routeFilters);
    }));

    const formFilters$ = combineLatest([
      this.form.valueChanges,
    ]).pipe(
      debounceTime(300),
      map(() => this.toFilter(this.form.getRawValue() as FilterFormValue)),
      distinctUntilChanged((a, b) => this.sameFilters(a, b)),
    );
    this.subscription.add(formFilters$.subscribe((formFilters) => {
      if (this.sameFilters(formFilters, this.lastSynced)) return;
      this.lastSynced = formFilters;
      this.filtersChange.emit(formFilters);
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: this.toQueryParams(formFilters),
        replaceUrl: true,
      });
    }));
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  reset(): void {
    this.form.reset({
      ville: '', dateDebut: null, dateFin: null, categorie: '', marque: '',
      carburant: '', transmission: '', prixMin: null, prixMax: null, sort: '',
    });
    this.drawerOpen = false;
  }

  toggleDrawer(): void {
    this.drawerOpen = !this.drawerOpen;
  }

  private fromQueryParams(params: Params): Partial<VehicleFilter> {
    return {
      ville: params['ville'] || undefined,
      dateDebut: params['dateDebut'] || undefined,
      dateFin: params['dateFin'] || undefined,
      categorie: params['categorie'] || undefined,
      marque: params['marque'] || undefined,
      carburant: params['carburant'] || undefined,
      transmission: params['transmission'] || undefined,
      prixMin: this.numberOrUndefined(params['prixMin']),
      prixMax: this.numberOrUndefined(params['prixMax']),
      sort: params['sort'] || undefined,
    };
  }

  private toFilter(value: FilterFormValue): Partial<VehicleFilter> {
    return {
      ville: value.ville.trim() || undefined,
      dateDebut: this.toIsoDate(value.dateDebut),
      dateFin: this.toIsoDate(value.dateFin),
      categorie: value.categorie || undefined,
      marque: value.marque || undefined,
      carburant: value.carburant || undefined,
      transmission: value.transmission || undefined,
      prixMin: value.prixMin ?? undefined,
      prixMax: value.prixMax ?? undefined,
      sort: value.sort || undefined,
    };
  }

  private toFormValue(filters: Partial<VehicleFilter>): Partial<FilterFormValue> {
    return {
      ...filters,
      dateDebut: this.parseDate(filters.dateDebut),
      dateFin: this.parseDate(filters.dateFin),
      carburant: filters.carburant ?? '',
      transmission: filters.transmission ?? '',
      ville: filters.ville ?? '',
      categorie: filters.categorie ?? '',
      marque: filters.marque ?? '',
      prixMin: filters.prixMin ?? null,
      prixMax: filters.prixMax ?? null,
      sort: filters.sort ?? '',
    };
  }

  private toQueryParams(filters: Partial<VehicleFilter>): Params {
    const params: Params = {};
    for (const [key, value] of Object.entries(filters)) {
      params[key] = value ?? null;
    }
    return params;
  }

  private numberOrUndefined(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private parseDate(value?: string): Date | null {
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private toIsoDate(value: Date | null): string | undefined {
    if (!value || Number.isNaN(value.getTime())) return undefined;
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private sameFilters(a: Partial<VehicleFilter>, b: Partial<VehicleFilter>): boolean {
    return JSON.stringify(this.toQueryParams(a)) === JSON.stringify(this.toQueryParams(b));
  }
}
