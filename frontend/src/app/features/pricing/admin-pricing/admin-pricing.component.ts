import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import {
  Discount,
  DiscountRequest,
  OptionPrice,
  OptionPriceRequest,
  PricingAdminService,
  Season,
  SeasonRequest,
} from '../services/pricing-admin.service';
import { SeasonFormModalComponent } from '../season-form-modal/season-form-modal.component';
import { OptionFormModalComponent } from '../option-form-modal/option-form-modal.component';
import { DiscountFormModalComponent } from '../discount-form-modal/discount-form-modal.component';
import { PricingSimulatorComponent } from '../pricing-simulator/pricing-simulator.component';

export interface AnnualCalendarDay {
  day: number;
  iso: string;
  color: string;
  tooltip: string;
  seasonName?: string;
}

export interface AnnualCalendarMonth {
  name: string;
  days: AnnualCalendarDay[];
}

const SEASON_PALETTE = ['#2563eb', '#176a50', '#d97706', '#b4532a', '#7c3aed', '#0891b2', '#be185d'];
const MONTH_NAMES = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];

@Component({
  selector: 'app-admin-pricing',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    PricingSimulatorComponent,
  ],
  templateUrl: './admin-pricing.component.html',
  styleUrl: './admin-pricing.component.scss',
})
export class AdminPricingComponent implements OnInit {
  private readonly pricingAdminService = inject(PricingAdminService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly seasons = signal<Season[]>([]);
  readonly options = signal<OptionPrice[]>([]);
  readonly discounts = signal<Discount[]>([]);
  readonly loading = signal<boolean>(true);
  readonly calendarYear = signal<number>(new Date().getFullYear());

  readonly seasonColumns = ['nom', 'dates', 'coefficient', 'priorite', 'actions'];
  readonly optionColumns = ['type', 'libelle', 'prixJour', 'actif', 'actions'];
  readonly discountColumns = ['libelle', 'type', 'pourcentage', 'conditions', 'actif', 'actions'];

  readonly seasonColorMap = computed(() => {
    const map = new Map<number, string>();
    this.seasons().forEach((s, idx) => {
      map.set(s.id, SEASON_PALETTE[idx % SEASON_PALETTE.length]);
    });
    return map;
  });

  readonly annualCalendar = computed<AnnualCalendarMonth[]>(() => {
    const year = this.calendarYear();
    const list = this.seasons();
    const colors = this.seasonColorMap();

    return MONTH_NAMES.map((name, monthIndex) => {
      const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
      const days: AnnualCalendarDay[] = [];

      for (let d = 1; d <= daysInMonth; d++) {
        const iso = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        const active = this.resolveSeasonForIso(iso, list);
        days.push({
          day: d,
          iso,
          color: active ? (colors.get(active.id) ?? '#cbd5e1') : '#eef2f6',
          seasonName: active?.nom,
          tooltip: active
            ? `${iso} · ${active.nom} (×${active.coefficient}, P${active.priorite})`
            : `${iso} · Tarif standard (×1.00)`,
        });
      }
      return { name, days };
    });
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    forkJoin({
      seasons: this.pricingAdminService.getSeasons(),
      options: this.pricingAdminService.getOptions(),
      discounts: this.pricingAdminService.getDiscounts(),
    }).subscribe({
      next: ({ seasons, options, discounts }) => {
        this.seasons.set(seasons ?? []);
        this.options.set(options ?? []);
        this.discounts.set(discounts ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger la configuration tarifaire.', 'Fermer', {
          duration: 3500,
        });
      },
    });
  }

  shiftCalendarYear(delta: number): void {
    this.calendarYear.update((y) => y + delta);
  }

  // ==================== SEASONS ====================

  openSeasonModal(season?: Season): void {
    const ref = this.dialog.open(SeasonFormModalComponent, {
      data: { season, existingSeasons: this.seasons() },
      width: '560px',
      maxWidth: '95vw',
    });

    ref.afterClosed().subscribe((payload: SeasonRequest | null) => {
      if (!payload) return;
      const request$ = season
        ? this.pricingAdminService.updateSeason(season.id, payload)
        : this.pricingAdminService.createSeason(payload);

      request$.subscribe({
        next: () => {
          this.snackBar.open(
            season ? 'Saison mise à jour.' : 'Saison créée avec succès.',
            'OK',
            { duration: 3000 },
          );
          this.refreshSeasons();
        },
        error: (err) => {
          this.snackBar.open(
            err?.error?.detail || 'Erreur lors de l’enregistrement de la saison.',
            'Fermer',
            { duration: 4000 },
          );
        },
      });
    });
  }

  deleteSeason(season: Season): void {
    this.pricingAdminService.deleteSeason(season.id).subscribe({
      next: () => {
        this.snackBar.open(`Saison "${season.nom}" supprimée.`, 'OK', { duration: 2500 });
        this.refreshSeasons();
      },
      error: () => {
        this.snackBar.open('Suppression de la saison impossible.', 'Fermer', { duration: 3500 });
      },
    });
  }

  private refreshSeasons(): void {
    this.pricingAdminService.getSeasons().subscribe({
      next: (list) => this.seasons.set(list ?? []),
    });
  }

  // ==================== OPTIONS ====================

  openOptionModal(option?: OptionPrice): void {
    const ref = this.dialog.open(OptionFormModalComponent, {
      data: {
        option,
        existingTypes: this.options().map((o) => o.type),
      },
      width: '480px',
      maxWidth: '95vw',
    });

    ref.afterClosed().subscribe((payload: OptionPriceRequest | null) => {
      if (!payload) return;
      const request$ = option
        ? this.pricingAdminService.updateOption(option.id, payload)
        : this.pricingAdminService.createOption(payload);

      request$.subscribe({
        next: () => {
          this.snackBar.open(
            option ? 'Option mise à jour.' : 'Option ajoutée avec succès.',
            'OK',
            { duration: 3000 },
          );
          this.refreshOptions();
        },
        error: (err) => {
          this.snackBar.open(
            err?.error?.detail || 'Erreur lors de l’enregistrement de l’option.',
            'Fermer',
            { duration: 4000 },
          );
        },
      });
    });
  }

  toggleOptionActive(option: OptionPrice, actif: boolean): void {
    const payload: OptionPriceRequest = {
      type: option.type,
      libelle: option.libelle || option.type,
      prixJour: option.prixJour,
      actif,
    };
    this.pricingAdminService.updateOption(option.id, payload).subscribe({
      next: (updated) => {
        this.options.update((list) => list.map((o) => (o.id === updated.id ? updated : o)));
        this.snackBar.open(
          `Option "${updated.libelle}" ${updated.actif ? 'activée' : 'désactivée'}.`,
          'OK',
          { duration: 2500 },
        );
      },
      error: () => {
        this.snackBar.open('Impossible de modifier le statut de l’option.', 'Fermer', {
          duration: 3500,
        });
      },
    });
  }

  deleteOption(option: OptionPrice): void {
    this.pricingAdminService.deleteOption(option.id).subscribe({
      next: () => {
        this.snackBar.open(`Option "${option.libelle || option.type}" supprimée.`, 'OK', {
          duration: 2500,
        });
        this.refreshOptions();
      },
      error: () => {
        this.snackBar.open('Suppression de l’option impossible.', 'Fermer', { duration: 3500 });
      },
    });
  }

  private refreshOptions(): void {
    this.pricingAdminService.getOptions().subscribe({
      next: (list) => this.options.set(list ?? []),
    });
  }

  // ==================== DISCOUNTS ====================

  openDiscountModal(discount?: Discount): void {
    const ref = this.dialog.open(DiscountFormModalComponent, {
      data: { discount },
      width: '540px',
      maxWidth: '95vw',
    });

    ref.afterClosed().subscribe((payload: DiscountRequest | null) => {
      if (!payload) return;
      const request$ = discount
        ? this.pricingAdminService.updateDiscount(discount.id, payload)
        : this.pricingAdminService.createDiscount(payload);

      request$.subscribe({
        next: () => {
          this.snackBar.open(
            discount ? 'Remise mise à jour.' : 'Remise créée avec succès.',
            'OK',
            { duration: 3000 },
          );
          this.refreshDiscounts();
        },
        error: (err) => {
          this.snackBar.open(
            err?.error?.detail || 'Erreur lors de l’enregistrement de la remise.',
            'Fermer',
            { duration: 4000 },
          );
        },
      });
    });
  }

  deleteDiscount(discount: Discount): void {
    this.pricingAdminService.deleteDiscount(discount.id).subscribe({
      next: () => {
        this.snackBar.open(`Remise "${discount.libelle}" supprimée.`, 'OK', { duration: 2500 });
        this.refreshDiscounts();
      },
      error: () => {
        this.snackBar.open('Suppression de la remise impossible.', 'Fermer', { duration: 3500 });
      },
    });
  }

  formatDiscountConditions(d: Discount): string {
    if (d.type === 'LONG_DURATION') {
      return d.minDays ? `Durée ≥ ${d.minDays} jours` : 'Longue durée';
    }
    const codePart = d.code ? `Code : ${d.code}` : 'Code promo';
    if (d.dateDebut && d.dateFin) {
      return `${codePart} (${d.dateDebut} → ${d.dateFin})`;
    }
    return codePart;
  }

  private refreshDiscounts(): void {
    this.pricingAdminService.getDiscounts().subscribe({
      next: (list) => this.discounts.set(list ?? []),
    });
  }

  private resolveSeasonForIso(iso: string, seasons: Season[]): Season | null {
    const md = iso.slice(5);
    const matching = seasons.filter((s) => {
      if (iso >= s.dateDebut && iso <= s.dateFin) return true;
      if (s.isRecurring) {
        const startMd = s.dateDebut.slice(5);
        const endMd = s.dateFin.slice(5);
        return startMd <= endMd ? md >= startMd && md <= endMd : md >= startMd || md <= endMd;
      }
      return false;
    });

    if (!matching.length) return null;
    return matching.reduce((best, curr) => {
      if (curr.priorite !== best.priorite) {
        return curr.priorite > best.priorite ? curr : best;
      }
      return curr.coefficient > best.coefficient ? curr : best;
    }, matching[0]);
  }
}
