import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import { AdminPricingComponent } from './admin-pricing.component';
import {
  Discount,
  OptionPrice,
  PricingAdminService,
  Season,
  SeasonRequest,
} from '../services/pricing-admin.service';
import { VehicleService } from '../../vehicles/services/vehicle.service';

const MOCK_SEASONS: Season[] = [
  {
    id: 1,
    nom: 'Haute saison été',
    dateDebut: '2026-07-01',
    dateFin: '2026-08-31',
    coefficient: 1.5,
    priorite: 3,
    isRecurring: true,
  },
];

const MOCK_OPTIONS: OptionPrice[] = [
  { id: 1, type: 'GPS', libelle: 'GPS Navigation', prixJour: 50, actif: true },
  { id: 2, type: 'BABY_SEAT', libelle: 'Siège bébé', prixJour: 30, actif: true },
];

const MOCK_DISCOUNTS: Discount[] = [
  {
    id: 1,
    type: 'LONG_DURATION',
    libelle: 'Remise > 7 jours',
    minDays: 8,
    pourcentage: 10,
    actif: true,
  },
];

describe('AdminPricingComponent', () => {
  let component: AdminPricingComponent;
  let fixture: ComponentFixture<AdminPricingComponent>;
  let pricingAdminSpy: jasmine.SpyObj<PricingAdminService>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    pricingAdminSpy = jasmine.createSpyObj('PricingAdminService', [
      'getSeasons',
      'createSeason',
      'updateSeason',
      'deleteSeason',
      'getOptions',
      'createOption',
      'updateOption',
      'deleteOption',
      'getDiscounts',
      'createDiscount',
      'updateDiscount',
      'deleteDiscount',
      'simulatePrice',
    ]);
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', ['search']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    pricingAdminSpy.getSeasons.and.returnValue(of(MOCK_SEASONS));
    pricingAdminSpy.getOptions.and.returnValue(of(MOCK_OPTIONS));
    pricingAdminSpy.getDiscounts.and.returnValue(of(MOCK_DISCOUNTS));
    pricingAdminSpy.simulatePrice.and.returnValue(
      of({
        sousTotal: 1600,
        options: 0,
        remiseDuree: 0,
        remisePromo: 0,
        remiseTotale: 0,
        caution: 3000,
        tva: 320,
        total: 4920,
        jours: 4,
      }),
    );
    vehicleServiceSpy.search.and.returnValue(
      of({ content: [], page: { size: 100, number: 0, totalElements: 0, totalPages: 0 }, totalElements: 0, totalPages: 0, size: 100, number: 0 }),
    );

    await TestBed.configureTestingModule({
      imports: [AdminPricingComponent, NoopAnimationsModule],
      providers: [
        { provide: PricingAdminService, useValue: pricingAdminSpy },
        { provide: VehicleService, useValue: vehicleServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    })
      .overrideComponent(AdminPricingComponent, {
        set: {
          providers: [
            { provide: MatDialog, useValue: dialogSpy },
            { provide: MatSnackBar, useValue: snackBarSpy },
          ],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AdminPricingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait charger les saisons, options, remises et afficher le calendrier annuel (12 mois)', () => {
    expect(component.seasons().length).toBe(1);
    expect(component.options().length).toBe(2);
    expect(component.discounts().length).toBe(1);
    expect(component.annualCalendar().length).toBe(12);
  });

  it('devrait créer une saison via la modale et mettre à jour le tableau (création saison -> tableau mis à jour)', () => {
    const newSeasonReq: SeasonRequest = {
      nom: 'Pic Fin Année',
      dateDebut: '2026-12-20',
      dateFin: '2026-12-31',
      coefficient: 2.0,
      priorite: 4,
      isRecurring: false,
    };
    const createdSeason: Season = { id: 2, ...newSeasonReq };

    dialogSpy.open.and.returnValue({
      afterClosed: () => of(newSeasonReq),
    } as MatDialogRef<unknown>);
    pricingAdminSpy.createSeason.and.returnValue(of(createdSeason));
    pricingAdminSpy.getSeasons.and.returnValue(of([...MOCK_SEASONS, createdSeason]));

    component.openSeasonModal();
    fixture.detectChanges();

    expect(pricingAdminSpy.createSeason).toHaveBeenCalledWith(newSeasonReq);
    expect(component.seasons().length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Pic Fin Année');
  });

  it('devrait basculer le statut actif/inactif d’une option', () => {
    const toggled: OptionPrice = { ...MOCK_OPTIONS[0], actif: false };
    pricingAdminSpy.updateOption.and.returnValue(of(toggled));

    component.toggleOptionActive(MOCK_OPTIONS[0], false);

    expect(pricingAdminSpy.updateOption).toHaveBeenCalledWith(1, {
      type: 'GPS',
      libelle: 'GPS Navigation',
      prixJour: 50,
      actif: false,
    });
    expect(component.options()[0].actif).toBeFalse();
  });
});
