import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { SaleListingContactService } from './sale-listing-contact.service';

describe('SaleListingContactService', () => {
  let service: SaleListingContactService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SaleListingContactService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('finds the published sale listing associated with a vehicle', () => {
    service.getPublishedListingIdByVehicle(7).subscribe((id) => expect(id).toBe(42));

    const request = httpMock.expectOne(
      (req) =>
        req.url === '/api/sale-listings' &&
        req.params.get('vehicleId') === '7' &&
        req.params.get('statut') === 'PUBLISHED',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ content: [{ id: 42 }] });
  });

  it('posts the request body to the listing contact endpoint', () => {
    const body = {
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: 'Question sur le véhicule',
    };

    service.sendContactRequest(42, body).subscribe((response) => expect(response.id).toBe(5));

    const request = httpMock.expectOne('/api/sale-listings/42/contact');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ id: 5, saleListingId: 42 });
  });
});
