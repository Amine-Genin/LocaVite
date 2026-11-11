import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ReservationService } from './reservation.service';
import { PriceQuote, QuoteRequest, Reservation, ReservationRequest } from '../models/reservation.model';

describe('ReservationService', () => {
  let service: ReservationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ReservationService],
    });
    service = TestBed.inject(ReservationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('devrait appeler POST /api/reservations/quote via getQuote()', () => {
    const request: QuoteRequest = {
      vehicleId: 1,
      dateDebut: '2026-11-15',
      dateFin: '2026-11-19',
      options: ['GPS'],
    };
    const mockQuote: PriceQuote = {
      sousTotal: 1400,
      options: 160,
      remise: 0,
      caution: 3000,
      tva: 312,
      total: 4872,
      jours: 4,
    };

    service.getQuote(request).subscribe((res) => {
      expect(res).toEqual(mockQuote);
    });

    const req = httpMock.expectOne('/api/reservations/quote');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockQuote);
  });

  it('devrait appeler GET /api/vehicles/:id/availability via checkAvailability()', () => {
    service.checkAvailability(1, '2026-11-15', '2026-11-19').subscribe((res) => {
      expect(res.unavailable.length).toBe(0);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/vehicles/1/availability' &&
        r.params.get('from') === '2026-11-15' &&
        r.params.get('to') === '2026-11-19',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ available: [{ from: '2026-11-15', to: '2026-11-19' }], unavailable: [] });
  });

  it('devrait appeler POST /api/reservations via create()', () => {
    const request: ReservationRequest = {
      vehicleId: 1,
      dateDebut: '2026-11-15',
      dateFin: '2026-11-19',
      lieuPriseEnCharge: 'Aéroport Mohammed V, Casablanca',
      lieuRetour: 'Agence LocaVite Casablanca',
      options: ['GPS', 'BABY_SEAT'],
    };
    const mockReservation: Reservation = {
      id: 42,
      code: 'LV-XYZ123',
      statut: 'PENDING',
      prixTotal: 5064,
    };

    service.create(request).subscribe((res) => {
      expect(res).toEqual(mockReservation);
    });

    const req = httpMock.expectOne('/api/reservations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockReservation);
  });
});
