import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { Subject, of, throwError } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';

import { VehicleListComponent } from './vehicle-list.component';
import { VehicleService } from '../services/vehicle.service';
import { Vehicle, VehiclePageResponse } from '../models/vehicle.model';
import { LanguageService } from '../../../core/i18n/language.service';
import { Router } from '@angular/router';
import { VehicleFiltersComponent } from '../vehicle-filters/vehicle-filters.component';

describe('VehicleListComponent', () => {
  let component: VehicleListComponent;
  let fixture: ComponentFixture<VehicleListComponent>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;

  const mockVehicles: Vehicle[] = [
    {
      id: 1,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      ville: 'Casablanca',
      prixJour: 350,
      photoUrl: 'https://example.com/duster.jpg',
    },
    {
      id: 2,
      marque: 'Renault',
      modele: 'Clio 5',
      annee: 2023,
      categorie: 'Citadine',
      ville: 'Marrakech',
      prixJour: 280,
      photoUrl: 'https://example.com/clio.jpg',
    },
    {
      id: 3,
      marque: 'Peugeot',
      modele: '208',
      annee: 2024,
      categorie: 'Citadine',
      ville: 'Rabat',
      prixJour: 300,
      photoUrl: null,
    },
  ];

  const mockPage0: VehiclePageResponse = {
    content: mockVehicles,
    page: {
      size: 12,
      number: 0,
      totalElements: 25,
      totalPages: 3,
    },
    totalElements: 25,
    totalPages: 3,
    size: 12,
    number: 0,
  };

  beforeEach(async () => {
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', [
      'search',
      'getBrands',
      'getCategories',
      'updateFilters',
      'resetFilters',
      'isFavorite',
      'toggleFavorite',
    ]);
    vehicleServiceSpy.search.and.returnValue(of(mockPage0));
    vehicleServiceSpy.getBrands.and.returnValue(of([]));
    vehicleServiceSpy.getCategories.and.returnValue(of([]));
    vehicleServiceSpy.isFavorite.and.returnValue(false);
    vehicleServiceSpy.toggleFavorite.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [VehicleListComponent, TranslateModule.forRoot()],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: VehicleService, useValue: vehicleServiceSpy },
        {
          provide: LanguageService,
          useValue: {
            currentLang: () => 'fr',
            direction: () => 'ltr',
            setLanguage: jasmine.createSpy('setLanguage'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleListComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display N vehicle cards when search returns N vehicles', () => {
    fixture.detectChanges();

    expect(vehicleServiceSpy.search).toHaveBeenCalledWith({ page: 0, size: 12 });
    expect(component.vehicles().length).toBe(mockVehicles.length);

    const cards = fixture.nativeElement.querySelectorAll('app-vehicle-card');
    expect(cards.length).toBe(mockVehicles.length);
  });

  it('should change page and call VehicleService.search with updated pagination parameters', () => {
    fixture.detectChanges();

    const page1Vehicles: Vehicle[] = [
      {
        id: 13,
        marque: 'Volkswagen',
        modele: 'Golf 8',
        annee: 2024,
        categorie: 'Berline',
        ville: 'Tanger',
        prixJour: 450,
      },
    ];
    const mockPage1: VehiclePageResponse = {
      content: page1Vehicles,
      page: {
        size: 12,
        number: 1,
        totalElements: 25,
        totalPages: 3,
      },
      totalElements: 25,
      totalPages: 3,
      size: 12,
      number: 1,
    };
    vehicleServiceSpy.search.and.returnValue(of(mockPage1));

    const pageEvent: PageEvent = {
      pageIndex: 1,
      pageSize: 12,
      length: 25,
    };
    component.onPageChange(pageEvent);
    fixture.detectChanges();

    expect(vehicleServiceSpy.search).toHaveBeenCalledWith({ page: 1, size: 12 });
    expect(component.pageIndex()).toBe(1);
    expect(component.vehicles().length).toBe(1);

    const cards = fixture.nativeElement.querySelectorAll('app-vehicle-card');
    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain('Volkswagen');
  });

  it('should display loading state (spinner and skeleton) while fetching vehicles', () => {
    const pendingSubject = new Subject<VehiclePageResponse>();
    vehicleServiceSpy.search.and.returnValue(pendingSubject.asObservable());

    fixture.detectChanges();

    expect(component.loading()).toBeTrue();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-cy="loading-spinner"]')).toBeTruthy();
    expect(el.querySelectorAll('[data-cy="skeleton-card"]').length).toBeGreaterThan(0);

    pendingSubject.next(mockPage0);
    pendingSubject.complete();
    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
    expect(el.querySelector('[data-cy="loading-spinner"]')).toBeNull();
    expect(el.querySelectorAll('app-vehicle-card').length).toBe(3);
  });

  it('should display empty state message when no vehicles are found', () => {
    vehicleServiceSpy.search.and.returnValue(
      of({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 12,
        number: 0,
      }),
    );

    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const emptyState = el.querySelector('[data-cy="empty-state"]');
    expect(emptyState).toBeTruthy();
    expect(emptyState?.textContent).toContain('Aucun véhicule disponible');
    expect(el.querySelectorAll('app-vehicle-card').length).toBe(0);
  });

  it('should handle error when search fails', () => {
    vehicleServiceSpy.search.and.returnValue(throwError(() => new Error('Server error')));

    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
    expect(component.error()).toBeTrue();
    expect(component.vehicles().length).toBe(0);
    expect(component.totalElements()).toBe(0);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-cy="empty-state"]')).toBeTruthy();
  });

  it('should toggle favorite status on favoriteToggle event', () => {
    fixture.detectChanges();

    vehicleServiceSpy.toggleFavorite.and.returnValue(true);
    component.onFavoriteToggle(mockVehicles[0]);

    expect(vehicleServiceSpy.toggleFavorite).toHaveBeenCalledWith(1);
    expect(component.vehicles()[0].isFavorite).toBeTrue();
  });

  it('should check if vehicle is favorite via isVehicleFavorite', () => {
    vehicleServiceSpy.isFavorite.and.returnValue(true);
    expect(component.isVehicleFavorite(mockVehicles[0])).toBeTrue();

    vehicleServiceSpy.isFavorite.and.returnValue(false);
    expect(component.isVehicleFavorite({ ...mockVehicles[0], isFavorite: true })).toBeTrue();
  });

  it('should apply partial filters and reset page to 0', () => {
    fixture.detectChanges();

    component.applyFilter({ ville: 'Casablanca' });

    expect(vehicleServiceSpy.updateFilters).toHaveBeenCalledWith({
      page: 0,
      size: 12,
      ville: 'Casablanca',
    });
    expect(component.pageIndex()).toBe(0);
  });

  it('should update results and URL after a debounced filter change', fakeAsync(() => {
    fixture.detectChanges();
    tick(300);
    flushMicrotasks();

    const filters = fixture.debugElement.query(By.directive(VehicleFiltersComponent))
      .componentInstance as VehicleFiltersComponent;
    const emitSpy = spyOn(filters.filtersChange, 'emit').and.callThrough();
    filters.form.controls.ville.setValue('Casablanca');
    expect(filters.form.controls.ville.value).toBe('Casablanca');
    fixture.detectChanges();
    tick(300);
    flushMicrotasks();
    fixture.detectChanges();

    expect(vehicleServiceSpy.search).toHaveBeenCalledWith(jasmine.objectContaining({
      page: 0,
      size: 12,
      ville: 'Casablanca',
    }));
    expect(TestBed.inject(Router).url).toContain('ville=Casablanca');
    expect(emitSpy).toHaveBeenCalledWith(jasmine.objectContaining({ ville: 'Casablanca' }));
    discardPeriodicTasks();
  }));

  it('should reset filters and trigger search', () => {
    fixture.detectChanges();

    component.resetFilters();

    expect(vehicleServiceSpy.resetFilters).toHaveBeenCalledWith(12);
    expect(component.pageIndex()).toBe(0);
  });
});
