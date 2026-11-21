import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { SaleListingService } from './sale-listing.service';

describe('SaleListingService', () => {
  let service: SaleListingService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SaleListingService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('searches published listings with filters and normalizes the paged response', () => {
    service
      .search({
        etat: 'USED',
        marque: 'Dacia',
        prixMin: 50000,
        anneeMin: 2020,
        carburant: 'DIESEL',
        page: 1,
        size: 8,
        sort: 'prix,asc',
      })
      .subscribe((page) => {
        expect(page.content.length).toBe(1);
        expect(page.totalElements).toBe(9);
        expect(page.totalPages).toBe(2);
        expect(page.number).toBe(1);
        expect(page.size).toBe(8);
      });

    const request = httpTesting.expectOne(
      (req) =>
        req.url === '/api/sale-listings/search' &&
        req.params.get('etat') === 'USED' &&
        req.params.get('marque') === 'Dacia' &&
        req.params.get('prixMin') === '50000' &&
        req.params.get('anneeMin') === '2020' &&
        req.params.get('carburant') === 'DIESEL' &&
        req.params.get('page') === '1' &&
        req.params.get('size') === '8' &&
        req.params.get('sort') === 'prix,asc',
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      content: [{ id: 4, vehicleId: 12, marque: 'Dacia', modele: 'Duster' }],
      totalElements: 9,
      totalPages: 2,
      number: 1,
      size: 8,
    });
  });
});
