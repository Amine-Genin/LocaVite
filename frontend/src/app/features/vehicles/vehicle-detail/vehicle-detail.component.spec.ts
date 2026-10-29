import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { VehicleDetailComponent } from './vehicle-detail.component';
import { VehicleService } from '../services/vehicle.service';
import { Vehicle } from '../models/vehicle.model';
import { VehicleContactDialogComponent } from './contact-dialog/vehicle-contact-dialog.component';

describe('VehicleDetailComponent', () => {
  let component: VehicleDetailComponent;
  let fixture: ComponentFixture<VehicleDetailComponent>;
  let router: Router;
  let vehicleService: jasmine.SpyObj<VehicleService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let titleService: Title;
  let metaService: Meta;

  const paramMap$ = new BehaviorSubject(convertToParamMap({ id: '1' }));

  const mockVehicle: Vehicle = {
    id: 1,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2024,
    categorie: 'SUV',
    ville: 'Casablanca',
    prixJour: 350,
    prixVente: 180000,
    carburant: 'DIESEL',
    transmission: 'MANUELLE',
    places: 5,
    description: 'SUV polyvalent et robuste, idéal pour les voyages.',
    matricule: '12345-A-1',
    agenceId: 1,
    agenceNom: 'LocaVite Casablanca Anfa',
    photoUrl: 'https://example.com/duster.jpg',
    photos: [
      { id: 10, vehicleId: 1, url: 'https://example.com/duster.jpg', isPrimary: true },
      { id: 11, vehicleId: 1, url: 'https://example.com/duster-side.jpg', isPrimary: false },
    ],
  };

  const mockSimilarVehicles: Vehicle[] = [
    {
      id: 2,
      marque: 'Renault',
      modele: 'Captur',
      annee: 2023,
      categorie: 'SUV',
      ville: 'Casablanca',
      prixJour: 400,
      photoUrl: 'https://example.com/captur.jpg',
    },
    {
      id: 3,
      marque: 'Peugeot',
      modele: '2008',
      annee: 2024,
      categorie: 'SUV',
      ville: 'Rabat',
      prixJour: 450,
      photoUrl: 'https://example.com/2008.jpg',
    },
  ];

  beforeEach(async () => {
    paramMap$.next(convertToParamMap({ id: '1' }));

    vehicleService = jasmine.createSpyObj('VehicleService', [
      'getById',
      'getAvailability',
      'search',
      'isFavorite',
      'toggleFavorite',
    ]);

    vehicleService.getById.and.returnValue(of(mockVehicle));
    vehicleService.getAvailability.and.returnValue(
      of({
        available: [{ from: '2026-11-01', to: '2026-11-10' }],
        unavailable: [{ from: '2026-11-11', to: '2026-11-15', reason: 'RESERVED' }],
      }),
    );
    vehicleService.search.and.returnValue(
      of({
        content: [mockVehicle, ...mockSimilarVehicles],
        page: { size: 10, number: 0, totalElements: 3, totalPages: 1 },
        totalElements: 3,
        totalPages: 1,
        size: 10,
        number: 0,
      }),
    );
    vehicleService.isFavorite.and.returnValue(false);
    vehicleService.toggleFavorite.and.returnValue(true);

    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(true),
    } as any);

    await TestBed.configureTestingModule({
      imports: [VehicleDetailComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: VehicleService, useValue: vehicleService },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMap$.asObservable(),
            snapshot: { paramMap: convertToParamMap({ id: '1' }) },
          },
        },
      ],
    })
      .overrideComponent(VehicleDetailComponent, {
        set: {
          providers: [{ provide: MatDialog, useValue: dialogSpy }],
        },
      })
      .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    titleService = TestBed.inject(Title);
    metaService = TestBed.inject(Meta);
    fixture = TestBed.createComponent(VehicleDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component and fetch vehicle by route param id', () => {
    expect(component).toBeTruthy();
    expect(vehicleService.getById).toHaveBeenCalledWith(1);
    expect(component.vehicle()?.modele).toBe('Duster');
    expect(component.loading()).toBeFalse();
  });

  it('should display all required vehicle data (brand, model, year, category, fuel, transmission, seats, prices, city, agency)', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    // Brand and model
    const brands = compiled.querySelectorAll('[data-cy="vehicle-brand"]');
    expect(brands.length).toBeGreaterThan(0);
    expect(brands[0].textContent).toContain('Dacia');

    const models = compiled.querySelectorAll('[data-cy="vehicle-model"]');
    expect(models.length).toBeGreaterThan(0);
    expect(models[0].textContent).toContain('Duster');

    // Year
    const years = compiled.querySelectorAll('[data-cy="vehicle-year"]');
    expect(years[0].textContent).toContain('2024');

    // Category
    const category = compiled.querySelector('[data-cy="vehicle-category"]');
    expect(category?.textContent).toContain('SUV');

    // Fuel & Transmission & Seats
    expect(compiled.querySelector('[data-cy="vehicle-fuel"]')?.textContent).toContain('DIESEL');
    expect(compiled.querySelector('[data-cy="vehicle-transmission"]')?.textContent).toContain(
      'MANUELLE',
    );
    expect(compiled.querySelector('[data-cy="vehicle-seats"]')?.textContent).toContain('5');

    // Description
    expect(compiled.querySelector('[data-cy="vehicle-description"]')?.textContent).toContain(
      'polyvalent',
    );

    // Rental price and sale price
    expect(compiled.querySelector('[data-cy="vehicle-rent-price"]')?.textContent).toContain('350');
    expect(compiled.querySelector('[data-cy="vehicle-sale-price"]')?.textContent).toContain(
      '180000',
    );

    // City and agency
    const cities = compiled.querySelectorAll('[data-cy="vehicle-city"]');
    expect(cities[0].textContent).toContain('Casablanca');

    const agencies = compiled.querySelectorAll('[data-cy="vehicle-agency"]');
    expect(agencies[0].textContent).toContain('LocaVite Casablanca Anfa');
  });

  it('should navigate to /reservation/:id when clicking reserve button', () => {
    component.onReserve();
    expect(router.navigate).toHaveBeenCalledWith(['/reservation', 1]);
  });

  it('should trigger navigation on click of Reserve button in DOM', () => {
    const reserveBtn = fixture.nativeElement.querySelector(
      '[data-cy="reserve-btn"]',
    ) as HTMLButtonElement;
    expect(reserveBtn).toBeTruthy();
    reserveBtn.click();
    expect(router.navigate).toHaveBeenCalledWith(['/reservation', 1]);
  });

  it('should toggle favorite status when favorite button is clicked', () => {
    const favoriteBtn = fixture.nativeElement.querySelector(
      '[data-cy="favorite-btn"]',
    ) as HTMLButtonElement;
    expect(favoriteBtn).toBeTruthy();

    favoriteBtn.click();
    expect(vehicleService.toggleFavorite).toHaveBeenCalledWith(1);
    expect(component.isFavorite()).toBeTrue();
  });

  it('should open contact dialog when contact agency button is clicked', () => {
    const contactBtn = fixture.nativeElement.querySelector(
      '[data-cy="contact-agency-btn"]',
    ) as HTMLButtonElement;
    expect(contactBtn).toBeTruthy();

    contactBtn.click();
    expect(dialogSpy.open).toHaveBeenCalledWith(VehicleContactDialogComponent, jasmine.any(Object));
  });

  it('should filter out the current vehicle and display up to 3 similar vehicles', () => {
    expect(component.similarVehicles().length).toBe(2);
    expect(component.similarVehicles().some((v) => v.id === 1)).toBeFalse();

    const similarCards = fixture.nativeElement.querySelectorAll(
      '[data-cy="similar-vehicles"] app-vehicle-card',
    );
    expect(similarCards.length).toBe(2);
  });

  it('should update dynamic SEO title and meta tags', () => {
    expect(titleService.getTitle()).toContain('Dacia Duster');
    expect(titleService.getTitle()).toContain('2024');

    const metaDesc = metaService.getTag("name='description'");
    expect(metaDesc?.content).toContain('SUV polyvalent');

    const ogTitle = metaService.getTag("property='og:title'");
    expect(ogTitle?.content).toContain('Dacia Duster');
  });

  it('should display not found state when vehicle is not found or error occurs', () => {
    vehicleService.getById.and.returnValue(throwError(() => new Error('Not found')));
    paramMap$.next(convertToParamMap({ id: '9999' }));
    fixture.detectChanges();

    expect(component.error()).toBeTrue();
    expect(component.loading()).toBeFalse();
    const notFoundEl = fixture.nativeElement.querySelector('[data-cy="not-found-state"]');
    expect(notFoundEl).toBeTruthy();
  });
});
