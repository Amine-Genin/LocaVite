import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { FAVORITES_STORAGE_KEY, VehicleService } from './vehicle.service';
import { VehicleCreateRequest, VehicleFilter } from '../models/vehicle.model';

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
      expect(res.content[0].photoUrl).toBe('https://example.com/duster.jpg');
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
          photos: [
            { id: 101, vehicleId: 1, url: 'https://example.com/duster.jpg', isPrimary: true },
          ],
        },
        {
          id: 2,
          marque: 'Peugeot',
          modele: '3008',
          annee: 2023,
          categorie: 'SUV',
          ville: 'Casablanca',
          prixJour: 500,
          photoUrl: 'https://example.com/peugeot.jpg',
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

  it('should handle search response without page metadata and empty filters', () => {
    service.search({}).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.totalElements).toBe(1);
      expect(res.totalPages).toBe(1);
      expect(res.size).toBe(12);
      expect(res.number).toBe(0);
    });

    const req = httpTesting.expectOne('/api/vehicles');
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [
        {
          id: 3,
          marque: 'Renault',
          modele: 'Clio',
          annee: 2022,
          categorie: 'Citadine',
          ville: 'Agadir',
          prixJour: 250,
        },
      ],
    });
  });

  it('should get vehicle by ID from /api/vehicles/:id', () => {
    service.getById(42).subscribe((res) => {
      expect(res.id).toBe(42);
      expect(res.marque).toBe('Mercedes');
      expect(res.modele).toBe('Classe A');
    });

    const req = httpTesting.expectOne('/api/vehicles/42');
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 42,
      marque: 'Mercedes',
      modele: 'Classe A',
      annee: 2024,
      categorie: 'Berline',
      ville: 'Rabat',
      prixJour: 700,
    });
  });

  it('should create vehicle via POST /api/vehicles', () => {
    const payload: VehicleCreateRequest = {
      brandId: 1,
      modele: 'Sandero',
      annee: 2024,
      categoryId: 2,
      ville: 'Casablanca',
      prixJour: 250,
      prixVente: 140000,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      description: 'Très bon état',
      matricule: '99999-B-1',
      statut: 'PUBLISHED',
    };

    service.create(payload).subscribe((res) => {
      expect(res.id).toBe(100);
      expect(res.modele).toBe('Sandero');
      expect(res.statut).toBe('PUBLISHED');
    });

    const req = httpTesting.expectOne('/api/vehicles');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({
      id: 100,
      brandId: 1,
      marque: 'Dacia',
      modele: 'Sandero',
      annee: 2024,
      categoryId: 2,
      categorie: 'Citadine',
      ville: 'Casablanca',
      prixJour: 250,
      prixVente: 140000,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      description: 'Très bon état',
      matricule: '99999-B-1',
      statut: 'PUBLISHED',
    });
  });

  it('should upload photos via POST /api/vehicles/:id/photos with FormData', () => {
    const fakeFile = new File(['dummy'], 'photo.jpg', { type: 'image/jpeg' });

    service.uploadPhotos(100, [fakeFile]).subscribe((photos) => {
      expect(photos.length).toBe(1);
      expect(photos[0].id).toBe(1);
      expect(photos[0].isPrimary).toBeTrue();
    });

    const req = httpTesting.expectOne('/api/vehicles/100/photos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush([{ id: 1, vehicleId: 100, url: 'https://example.com/p1.jpg', isPrimary: true }]);
  });

  it('should set primary photo via PUT /api/vehicles/:vehicleId/photos/:photoId/primary', () => {
    service.setPrimaryPhoto(100, 2).subscribe((res) => {
      expect(res.id).toBe(2);
      expect(res.isPrimary).toBeTrue();
    });

    const req = httpTesting.expectOne('/api/vehicles/100/photos/2/primary');
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 2, vehicleId: 100, url: 'https://example.com/p2.jpg', isPrimary: true });
  });

  it('should delete photo via DELETE /api/vehicles/:vehicleId/photos/:photoId', () => {
    service.deletePhoto(100, 2).subscribe();

    const req = httpTesting.expectOne('/api/vehicles/100/photos/2');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should fetch brands via GET /api/brands', () => {
    service.getBrands().subscribe((brands) => {
      expect(brands.length).toBe(2);
      expect(brands[0].name).toBe('Dacia');
    });

    const req = httpTesting.expectOne('/api/brands');
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, name: 'Dacia' },
      { id: 2, name: 'Renault' },
    ]);
  });

  it('should fetch categories via GET /api/categories', () => {
    service.getCategories().subscribe((cats) => {
      expect(cats.length).toBe(2);
      expect(cats[0].name).toBe('SUV');
    });

    const req = httpTesting.expectOne('/api/categories');
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, name: 'SUV' },
      { id: 2, name: 'Citadine' },
    ]);
  });

  it('should update and reset filters BehaviorSubject', () => {
    service.updateFilters({ ville: 'Marrakech', page: 2 });
    expect(service.currentFilters).toEqual({ page: 2, size: 12, ville: 'Marrakech' });

    service.resetFilters(12);
    expect(service.currentFilters).toEqual({ page: 0, size: 12 });

    service.resetFilters();
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

  it('should handle malformed JSON in localStorage gracefully without throwing', () => {
    localStorage.setItem(FAVORITES_STORAGE_KEY, 'not-valid-json');
    const newService = TestBed.inject(VehicleService);
    expect(newService.isFavorite(99)).toBeFalse();
  });
});
