import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { BehaviorSubject, of } from 'rxjs';

import { VehicleHistoryService } from '../../vehicles/services/vehicle-history.service';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { SaleListing, SaleListingPage } from '../models/sale-listing.model';
import { SaleListingService } from '../services/sale-listing.service';
import { ContactModalComponent } from './contact-modal/contact-modal.component';
import { SaleDetailComponent } from './sale-detail.component';
import { VisitModalComponent } from './visit-modal/visit-modal.component';

describe('SaleDetailComponent', () => {
  let fixture: ComponentFixture<SaleDetailComponent>;
  let saleListingService: jasmine.SpyObj<SaleListingService>;
  let vehicleService: jasmine.SpyObj<VehicleService>;
  let historyService: jasmine.SpyObj<VehicleHistoryService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const routeParamMap = new BehaviorSubject(convertToParamMap({ id: '31' }));
  const listing: SaleListing = {
    id: 31,
    vehicleId: 12,
    titre: 'Dacia Duster',
    description: 'SUV polyvalent et robuste.',
    prixVente: 185000,
    prixNego: true,
    etat: 'USED',
    kilometrage: 52000,
    numeroChassis: 'VIN-123',
    nombreProprietaires: 1,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2022,
    ville: 'Casablanca',
    carburant: 'DIESEL',
    transmission: 'MANUELLE',
    categorie: 'SUV',
    photoUrl: 'https://example.com/duster.jpg',
    publishedAt: '2026-03-01T10:00:00Z',
  };

  beforeEach(async () => {
    saleListingService = jasmine.createSpyObj<SaleListingService>('SaleListingService', [
      'getById',
      'search',
    ]);
    vehicleService = jasmine.createSpyObj<VehicleService>('VehicleService', [
      'getPhotos',
      'isFavorite',
      'toggleFavorite',
    ]);
    historyService = jasmine.createSpyObj<VehicleHistoryService>('VehicleHistoryService', [
      'getHistoryForListing',
    ]);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    saleListingService.getById.and.returnValue(of(listing));
    saleListingService.search.and.returnValue(
      of({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 100,
      } satisfies SaleListingPage),
    );
    vehicleService.getPhotos.and.returnValue(of([]));
    vehicleService.isFavorite.and.returnValue(false);
    vehicleService.toggleFavorite.and.returnValue(true);
    historyService.getHistoryForListing.and.returnValue(of([]));
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as MatDialogRef<unknown>);

    await TestBed.configureTestingModule({
      imports: [SaleDetailComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: routeParamMap.asObservable() },
        },
        { provide: SaleListingService, useValue: saleListingService },
        { provide: VehicleService, useValue: vehicleService },
        { provide: VehicleHistoryService, useValue: historyService },
      ],
    })
      .overrideComponent(SaleDetailComponent, {
        set: { providers: [{ provide: MatDialog, useValue: dialog }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SaleDetailComponent);
    fixture.detectChanges();
  });

  it('loads and displays the listing details', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(saleListingService.getById).toHaveBeenCalledWith(31);
    expect(element.querySelector('[data-cy="sale-detail-title"]')?.textContent).toContain(
      'Dacia Duster',
    );
    expect(element.querySelector('[data-cy="sale-detail-year"]')?.textContent).toContain('2022');
    expect(element.querySelector('[data-cy="sale-detail-vin"]')?.textContent).toContain('VIN-123');
    expect(element.querySelector('[data-cy="sale-detail-description"]')?.textContent).toContain(
      'polyvalent',
    );
    expect(element.querySelector('[data-cy="sale-detail-price"]')?.textContent).toContain('185');
    expect(historyService.getHistoryForListing).toHaveBeenCalledWith(31);
  });

  it('opens the contact modal for this listing', () => {
    (
      fixture.nativeElement.querySelector('[data-cy="sale-contact-open"]') as HTMLButtonElement
    ).click();

    expect(dialog.open).toHaveBeenCalledWith(
      ContactModalComponent,
      jasmine.objectContaining({ data: { listing } }),
    );
  });

  it('opens the visit modal for this listing', () => {
    (
      fixture.nativeElement.querySelector('[data-cy="sale-visit-open"]') as HTMLButtonElement
    ).click();

    expect(dialog.open).toHaveBeenCalledWith(
      VisitModalComponent,
      jasmine.objectContaining({ data: { listing } }),
    );
  });
});
