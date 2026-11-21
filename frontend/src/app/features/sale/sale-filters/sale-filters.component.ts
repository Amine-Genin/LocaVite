import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  inject,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { TranslateModule } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, map, Subscription } from 'rxjs';

import { CatalogEntry } from '../../vehicles/models/vehicle.model';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { SaleCondition, SaleFuel, SaleListingFilters } from '../models/sale-listing.model';

interface SaleFilterForm {
  etat: FormControl<SaleCondition | ''>;
  marque: FormControl<string>;
  categorie: FormControl<string>;
  prixMin: FormControl<number | null>;
  prixMax: FormControl<number | null>;
  anneeMin: FormControl<number | null>;
  anneeMax: FormControl<number | null>;
  kilometrageMax: FormControl<number | null>;
  carburant: FormControl<SaleFuel | ''>;
  ville: FormControl<string>;
}

@Component({
  selector: 'app-sale-filters',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    TranslateModule,
  ],
  templateUrl: './sale-filters.component.html',
  styleUrl: './sale-filters.component.scss',
})
export class SaleFiltersComponent implements OnChanges, OnDestroy {
  private readonly vehicleService = inject(VehicleService);
  private readonly subscription = new Subscription();

  @Input() filters: Partial<SaleListingFilters> = {};
  @Output() readonly filtersChange = new EventEmitter<Partial<SaleListingFilters>>();

  readonly brands: CatalogEntry[] = [];
  readonly categories: CatalogEntry[] = [];
  readonly fuels: SaleFuel[] = ['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE'];
  catalogError = false;
  readonly form = new FormGroup<SaleFilterForm>({
    etat: new FormControl<SaleCondition | ''>('', { nonNullable: true }),
    marque: new FormControl('', { nonNullable: true }),
    categorie: new FormControl('', { nonNullable: true }),
    prixMin: new FormControl<number | null>(null),
    prixMax: new FormControl<number | null>(null),
    anneeMin: new FormControl<number | null>(null),
    anneeMax: new FormControl<number | null>(null),
    kilometrageMax: new FormControl<number | null>(null),
    carburant: new FormControl<SaleFuel | ''>('', { nonNullable: true }),
    ville: new FormControl('', { nonNullable: true }),
  });

  drawerOpen = false;

  constructor() {
    this.subscription.add(
      this.vehicleService.getBrands().subscribe({
        next: (brands) => this.brands.splice(0, this.brands.length, ...brands),
        error: () => (this.catalogError = true),
      }),
    );
    this.subscription.add(
      this.vehicleService.getCategories().subscribe({
        next: (categories) => this.categories.splice(0, this.categories.length, ...categories),
        error: () => (this.catalogError = true),
      }),
    );
    this.subscription.add(
      this.form.valueChanges
        .pipe(
          debounceTime(250),
          map(() => this.toFilters()),
          distinctUntilChanged((first, second) => JSON.stringify(first) === JSON.stringify(second)),
        )
        .subscribe((filters) => this.filtersChange.emit(filters)),
    );
  }

  ngOnChanges(): void {
    this.form.patchValue(
      {
        etat: this.filters.etat ?? '',
        marque: this.filters.marque ?? '',
        categorie: this.filters.categorie ?? '',
        prixMin: this.filters.prixMin ?? null,
        prixMax: this.filters.prixMax ?? null,
        anneeMin: this.filters.anneeMin ?? null,
        anneeMax: this.filters.anneeMax ?? null,
        kilometrageMax: this.filters.kilometrageMax ?? null,
        carburant: this.filters.carburant ?? '',
        ville: this.filters.ville ?? '',
      },
      { emitEvent: false },
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  reset(): void {
    this.form.reset({
      etat: '',
      marque: '',
      categorie: '',
      prixMin: null,
      prixMax: null,
      anneeMin: null,
      anneeMax: null,
      kilometrageMax: null,
      carburant: '',
      ville: '',
    });
    this.drawerOpen = false;
  }

  toggleDrawer(): void {
    this.drawerOpen = !this.drawerOpen;
  }

  private toFilters(): Partial<SaleListingFilters> {
    const value = this.form.getRawValue();
    return {
      etat: value.etat || undefined,
      marque: value.marque.trim() || undefined,
      categorie: value.categorie || undefined,
      prixMin: value.prixMin ?? undefined,
      prixMax: value.prixMax ?? undefined,
      anneeMin: value.anneeMin ?? undefined,
      anneeMax: value.anneeMax ?? undefined,
      kilometrageMax: value.kilometrageMax ?? undefined,
      carburant: value.carburant || undefined,
      ville: value.ville.trim() || undefined,
    };
  }
}
