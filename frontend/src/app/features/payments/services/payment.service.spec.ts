import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { PaymentService } from './payment.service';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PaymentService],
    });

    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('initiate() avec CMI appelle POST /api/payments/cmi/initiate et retourne cmiRedirectUrl', () => {
    service
      .initiate({
        reservationId: 88,
        montant: 1400,
        methode: 'CMI',
      })
      .subscribe((res) => {
        expect(res.id).toBe(42);
        expect(res.code).toBe('PAY-CMI042');
        expect(res.cmiRedirectUrl).toContain('https://testpayment.cmi.co.ma');
      });

    const req = httpMock.expectOne('/api/payments/cmi/initiate');
    expect(req.request.method).toBe('POST');
    req.flush({
      paymentId: 42,
      paymentCode: 'PAY-CMI042',
      montant: 1400,
      devise: 'MAD',
      gatewayUrl: 'https://testpayment.cmi.co.ma/fim/est3Dgate',
      redirectUrl: 'https://testpayment.cmi.co.ma/fim/est3Dgate?oid=PAY-CMI042',
    });
  });

  it('getStatus() appelle GET /api/payments/:id et enrichit invoiceNumber', () => {
    service.getStatus(42).subscribe((res) => {
      expect(res.statut).toBe('SUCCESS');
      expect(res.invoiceNumber).toBe('FAC-2026-00042');
    });

    const req = httpMock.expectOne('/api/payments/42');
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 42,
      code: 'PAY-CMI042',
      reservationId: 88,
      montant: 1400,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      paidAt: '2026-11-14T22:00:00Z',
    });
  });
});
