import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { VehicleService } from '../../vehicles/services/vehicle.service';
import { SaleListingService } from '../services/sale-listing.service';
import { SaleFormComponent } from './sale-form.component';

describe('SaleFormComponent', () => {
  let fixture: ComponentFixture<SaleFormComponent>;
  let component: SaleFormComponent;
  let vehicleService: jasmine.SpyObj<VehicleService>;
  let saleListingService: jasmine.SpyObj<SaleListingService>;
  let router: Router;

  beforeEach(async () => {
    vehicleService = jasmine.createSpyObj<VehicleService>('VehicleService', [
      'search',
      'getBrands',
      'getCategories',
      'create',
      'uploadPhotos',
      'setPrimaryPhoto',
    ]);
    saleListingService = jasmine.createSpyObj<SaleListingService>('SaleListingService', [
      'create',
      'estimatePrice',
    ]);

    vehicleService.search.and.returnValue(
      of({
        content: [
          {
            id: 12,
            marque: 'Dacia',
            modele: 'Duster',
            annee: 2022,
            categorie: 'SUV',
            ville: 'Casablanca',
            prixJour: 350,
            prixVente: 185000,
            carburant: 'DIESEL',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 100,
        number: 0,
      }),
    );
    vehicleService.getBrands.and.returnValue(of([{ id: 1, name: 'Dacia' }]));
    vehicleService.getCategories.and.returnValue(of([{ id: 1, name: 'SUV' }]));
    vehicleService.create.and.returnValue(
      of({
        id: 12,
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2022,
        categorie: 'SUV',
        ville: 'Casablanca',
        prixJour: 350,
      }),
    );
    vehicleService.uploadPhotos.and.returnValue(of([]));
    vehicleService.setPrimaryPhoto.and.returnValue(
      of({ id: 101, vehicleId: 12, url: '/photo.jpg', isPrimary: true }),
    );
    saleListingService.create.and.returnValue(
      of({
        id: 31,
        vehicleId: 12,
        titre: 'Dacia Duster',
        description: 'Excellent état',
        prixVente: 185000,
        prixNego: true,
        etat: 'USED',
        kilometrage: 52000,
        premierProprietaire: true,
        dateMiseEnCirculation: '2022-03-01',
        numeroChassis: 'VIN-123',
        nombreProprietaires: 1,
        statut: 'PUBLISHED',
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2022,
        ville: 'Casablanca',
        carburant: 'DIESEL',
        transmission: 'MANUELLE',
        categorie: 'SUV',
        photoUrl: null,
        publishedAt: '2026-11-22',
      }),
    );
    saleListingService.estimatePrice.and.returnValue(
      of({
        estimatedPrice: 180000,
        minimumPrice: 170000,
        maximumPrice: 190000,
        comparableListings: 5,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [SaleFormComponent, TranslateModule.forRoot()],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: VehicleService, useValue: vehicleService },
        { provide: SaleListingService, useValue: saleListingService },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    fixture = TestBed.createComponent(SaleFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('creates a listing from a valid existing vehicle form', () => {
    component.vehicleForm.controls.vehicleId.setValue(12);
    component.conditionForm.controls.etat.setValue('USED');
    component.priceForm.patchValue({ prixVente: 185000, prixNego: true });
    component.detailsForm.patchValue({
      numeroChassis: 'VIN-123',
      kilometrage: 52000,
      dateMiseEnCirculation: new Date('2022-03-01'),
      nombreProprietaires: 1,
    });
    component.descriptionForm.patchValue({
      titre: 'Dacia Duster',
      description: 'Excellent état',
    });
    fixture.detectChanges();

    component.submit('PUBLISHED');

    expect(saleListingService.create).toHaveBeenCalled();
    expect(saleListingService.create.calls.mostRecent().args[0].vehicleId).toBe(12);
    expect(saleListingService.create.calls.mostRecent().args[0].statut).toBe('PUBLISHED');
  });

  it('updates conditional fields for NEW and USED states', () => {
    component.conditionForm.controls.etat.setValue('NEW');
    expect(component.detailsForm.controls.kilometrage.value).toBe(0);
    expect(component.detailsForm.controls.kilometrage.disabled).toBeTrue();

    component.conditionForm.controls.etat.setValue('USED');
    expect(component.detailsForm.controls.kilometrage.enabled).toBeTrue();
    expect(component.detailsForm.controls.dateMiseEnCirculation.enabled).toBeTrue();
  });

  it('displays an indicative estimate returned by the service', () => {
    component.vehicleForm.controls.vehicleId.setValue(12);
    component.estimatePrice();
    expect(saleListingService.estimatePrice).toHaveBeenCalled();
    expect(component.estimate()?.estimatedPrice).toBe(180000);

    fixture.detectChanges();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-cy="sale-estimate-result"]'),
    ).toBeTruthy();
  });

});
