import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { VehicleHistoryService } from './vehicle-history.service';

describe('VehicleHistoryService', () => {
  let service: VehicleHistoryService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(VehicleHistoryService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('finds the published used listing for a vehicle and loads its timeline', () => {
    service.getHistoryForVehicle(12).subscribe((result) => {
      expect(result.hasListing).toBeTrue();
      expect(result.events.length).toBe(1);
      expect(result.events[0].typeEvenement).toBe('VIDANGE');
    });

    const listingRequest = httpTesting.expectOne(
      (request) =>
        request.url === '/api/sale-listings' &&
        request.params.get('vehicleId') === '12' &&
        request.params.get('etat') === 'USED' &&
        request.params.get('statut') === 'PUBLISHED',
    );
    expect(listingRequest.request.method).toBe('GET');
    listingRequest.flush({ content: [{ id: 33, etat: 'USED', statut: 'PUBLISHED' }] });

    const historyRequest = httpTesting.expectOne('/api/sale-listings/33/history');
    expect(historyRequest.request.method).toBe('GET');
    historyRequest.flush([
      {
        id: 3,
        saleListingId: 33,
        vehicleId: 12,
        typeEvenement: 'VIDANGE',
        date: '2024-06-15',
        description: 'Entretien',
        kilometrage: 42000,
        verified: false,
      },
    ]);
  });

  it('does not request a timeline when there is no published used listing', () => {
    service.getHistoryForVehicle(12).subscribe((result) => {
      expect(result).toEqual({ hasListing: false, events: [] });
    });

    const listingRequest = httpTesting.expectOne(
      (request) => request.url === '/api/sale-listings' && request.params.get('vehicleId') === '12',
    );
    listingRequest.flush({ content: [{ id: 34, etat: 'NEW', statut: 'PUBLISHED' }] });
    httpTesting.expectNone('/api/sale-listings/34/history');
  });
});
