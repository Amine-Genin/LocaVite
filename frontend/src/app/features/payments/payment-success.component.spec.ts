import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';

import { PaymentSuccessComponent } from './payment-success.component';
import { PaymentService } from './services/payment.service';
import { PaymentResponse } from './models/payment.model';

describe('PaymentSuccessComponent', () => {
  let component: PaymentSuccessComponent;
  let fixture: ComponentFixture<PaymentSuccessComponent>;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;

  const mockSuccessPayment: PaymentResponse = {
    id: 42,
    code: 'PAY-CMI042',
    reservationId: 88,
    montant: 1400,
    devise: 'MAD',
    methode: 'CMI',
    statut: 'SUCCESS',
    invoiceNumber: 'FAC-2026-00042',
    paidAt: '2026-11-14T22:00:00Z',
  };

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj<PaymentService>('PaymentService', [
      'pollStatus',
      'getStatus',
      'downloadInvoice',
      'extractInvoiceNumber',
      'isTerminalStatus',
    ]);

    paymentServiceSpy.pollStatus.and.returnValue(of(mockSuccessPayment));
    paymentServiceSpy.getStatus.and.returnValue(of(mockSuccessPayment));
    paymentServiceSpy.extractInvoiceNumber.and.returnValue('FAC-2026-00042');
    paymentServiceSpy.isTerminalStatus.and.returnValue(true);
    paymentServiceSpy.downloadInvoice.and.returnValue(
      of(new Blob(['%PDF-1.4'], { type: 'application/pdf' })),
    );

    await TestBed.configureTestingModule({
      imports: [
        PaymentSuccessComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        { provide: PaymentService, useValue: paymentServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                paymentId: '42',
                reservationId: '88',
                code: 'PAY-CMI042',
                method: 'CMI',
              }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('Test : page succès affiche le numéro de facture et le bouton télécharger', () => {
    const el: HTMLElement = fixture.nativeElement;
    const invoiceEl = el.querySelector('[data-cy="invoice-number"]');
    const downloadBtn = el.querySelector('[data-cy="download-invoice-btn"]');

    expect(invoiceEl).toBeTruthy();
    expect(invoiceEl?.textContent?.trim()).toBe('FAC-2026-00042');
    expect(downloadBtn).toBeTruthy();
  });

  it('devrait déclencher le téléchargement du PDF de la facture au clic sur le bouton', () => {
    component.downloadInvoice();
    expect(paymentServiceSpy.downloadInvoice).toHaveBeenCalledWith(42, 'FR');
  });
});
