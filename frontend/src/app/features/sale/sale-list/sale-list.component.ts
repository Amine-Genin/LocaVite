import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription, distinctUntilChanged, map } from 'rxjs';

import {
  SaleCondition,
  SaleFuel,
  SaleListing,
  SaleListingFilters,
} from '../models/sale-listing.model';
import { SaleListingService } from '../services/sale-listing.service';
import { SaleCardComponent } from '../sale-card/sale-card.component';
import { SaleFiltersComponent } from '../sale-filters/sale-filters.component';

@Component({
  selector: 'app-sale-list',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    TranslateModule,
    SaleCardComponent,
    SaleFiltersComponent,
  ],
  templateUrl: './sale-list.component.html',
  styleUrl: './sale-list.component.scss',
})
export class SaleListComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly saleListingService = inject(SaleListingService);
  private readonly subscription = new Subscription();
  private requestSubscription?: Subscription;

  readonly pageSizeOptions = [8, 12, 24];
  readonly filters = signal<SaleListingFilters>({
    page: 0,
    size: 12,
    sort: 'datePublication,desc',
  });
  readonly listings = signal<SaleListing[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  readonly error = signal(false);

  ngOnInit(): void {
    this.subscription.add(
      this.route.queryParams
        .pipe(
          map((params) => this.filtersFromParams(params)),
          distinctUntilChanged((first, second) => JSON.stringify(first) === JSON.stringify(second)),
        )
        .subscribe((filters) => {
          this.filters.set(filters);
          this.search(filters);
        }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.requestSubscription?.unsubscribe();
  }

  onFiltersChange(partial: Partial<SaleListingFilters>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        ...this.toQueryParams(this.filters()),
        ...this.toQueryParams(partial),
        page: 0,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  onPageChange(event: PageEvent): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: event.pageIndex, size: event.pageSize },
      queryParamsHandling: 'merge',
    });
  }

  resetFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        etat: null,
        marque: null,
        categorie: null,
        prixMin: null,
        prixMax: null,
        anneeMin: null,
        anneeMax: null,
        kilometrageMax: null,
        carburant: null,
        ville: null,
        page: 0,
        sort: 'datePublication,desc',
      },
      queryParamsHandling: 'merge',
    });
  }

  private search(filters: SaleListingFilters): void {
    this.loading.set(true);
    this.error.set(false);
    this.requestSubscription?.unsubscribe();
    this.requestSubscription = this.saleListingService.search(filters).subscribe({
      next: (page) => {
        this.listings.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.listings.set([]);
        this.totalElements.set(0);
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  private filtersFromParams(params: Params): SaleListingFilters {
    const condition = params['etat'];
    const fuel = params['carburant'];
    const sort = params['sort'];
    return {
      etat: condition === 'NEW' || condition === 'USED' ? (condition as SaleCondition) : undefined,
      marque: params['marque'] || undefined,
      categorie: params['categorie'] || undefined,
      prixMin: this.numberOrUndefined(params['prixMin']),
      prixMax: this.numberOrUndefined(params['prixMax']),
      anneeMin: this.numberOrUndefined(params['anneeMin']),
      anneeMax: this.numberOrUndefined(params['anneeMax']),
      kilometrageMax: this.numberOrUndefined(params['kilometrageMax']),
      carburant: ['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE'].includes(fuel)
        ? (fuel as SaleFuel)
        : undefined,
      ville: params['ville'] || undefined,
      page: Math.max(0, this.numberOrUndefined(params['page']) ?? 0),
      size: this.pageSizeOptions.includes(this.numberOrUndefined(params['size']) ?? 12)
        ? (this.numberOrUndefined(params['size']) ?? 12)
        : 12,
      sort: [
        'prix,asc',
        'prix,desc',
        'annee,desc',
        'kilometrage,asc',
        'datePublication,desc',
      ].includes(sort)
        ? sort
        : 'datePublication,desc',
    };
  }

  private toQueryParams(filters: Partial<SaleListingFilters>): Params {
    return Object.fromEntries(Object.entries(filters).map(([key, value]) => [key, value ?? null]));
  }

  private numberOrUndefined(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
}
