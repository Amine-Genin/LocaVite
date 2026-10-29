import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { FAVORITES_STORAGE_KEY, VehicleService } from './vehicle.service';
import { VehicleFilter } from '../models/vehicle.model';

describe('VehicleService', () => {
  let service: VehicleService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(FAVORITES_STORAGE_KEY);

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(VehicleService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.removeItem(FAVORITES_STORAGE_KEY);
  });

  it('should be created with default filters BehaviorSubject', () => {
    expect(service).toBeTruthy();
    expect(service.currentFilters).toEqual({ page: 0, size: 12 });
  });

  it('should call GET /api/vehicles with filter query params and normalize PagedModel response', () => {
    const filter: VehicleFilter = {
      ville: 'Casablanca',
      categorie: 'SUV',
      page: 1,
      size: 12,
    };

    service.search(filter).subscribe((res) => {
      expect(res.content.length).toBe(2);
      expect(res.totalElements).toBe(25);
      expect(res.totalPages).toBe(3);
      expect(res.number).toBe(1);
      expect(res.size).toBe(12);
      expect(res.content[0].marque).toBe('Dacia');
    });

    const req = httpTesting.expectOne(
      (r) =>
        r.url === '/api/vehicles' &&
        r.params.get('ville') === 'Casablanca' &&
        r.params.get('categorie') === 'SUV' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '12',
    );
    expect(req.request.method).toBe('GET');

    req.flush({
      content: [
        {
          id: 1,
          marque: 'Dacia',
          modele: 'Duster',
          annee: 2024,
          categorie: 'SUV',
          ville: 'Casablanca',
          prixJour: 350,
        },
        {
          id: 2,
          marque: 'Peugeot',
          modele: '3008',
          annee: 2023,
          categorie: 'SUV',
          ville: 'Casablanca',
          prixJour: 500,
        },
      ],
      page: {
        size: 12,
        number: 1,
        totalElements: 25,
        totalPages: 3,
      },
    });
  });

  it('should update and reset filters BehaviorSubject', () => {
    service.updateFilters({ ville: 'Marrakech', page: 2 });
    expect(service.currentFilters).toEqual({ page: 2, size: 12, ville: 'Marrakech' });

    service.resetFilters(12);
    expect(service.currentFilters).toEqual({ page: 0, size: 12 });
  });

  it('should toggle favorite state and persist in localStorage', () => {
    expect(service.isFavorite(10)).toBeFalse();

    const added = service.toggleFavorite(10);
    expect(added).toBeTrue();
    expect(service.isFavorite(10)).toBeTrue();

    const removed = service.toggleFavorite(10);
    expect(removed).toBeFalse();
    expect(service.isFavorite(10)).toBeFalse();
  });
});
