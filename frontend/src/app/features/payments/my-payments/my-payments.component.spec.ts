import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';

import { MyPaymentsComponent } from './my-payments.component';
import { PaymentService } from '../services/payment.service';
import { PaymentPageResponse, PaymentResponse } from '../models/payment.model';

describe('MyPaymentsComponent', () => {
  let component: MyPaymentsComponent;
  let fixture: ComponentFixture<MyPaymentsComponent>;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let router: Router;

  const mockPayments: PaymentResponse[] = [
    {
      id: 101,
      code: 'PAY-CMI101',
      reservationId: 88,
      montant: 1400,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      invoiceNumber: 'FAC-2026-00101',
      createdAt: '2026-11-10T10:30:00Z',
      paidAt: '2026-11-10T10:31:00Z',
    },
    {
      id: 102,
      code: 'PAY-CSH102',
      reservationId: 89,
      montant: 900,
      devise: 'MAD',
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
      invoiceNumber: 'FAC-2026-00102',
      createdAt: '2026-11-12T14:00:00Z',
    },
    {
      id: 103,
      code: 'PAY-VIR103',
      reservationId: 90,
      montant: 2100,
      devise: 'MAD',
      methode: 'BANK_TRANSFER',
      statut: 'FAILED',
      invoiceNumber: 'FAC-2026-00103',
      createdAt: '2026-11-14T09:15:00Z',
    },
  ];

  const mockPage: PaymentPageResponse = {
    content: mockPayments,
    totalElements: 3,
    totalPages: 1,
    size: 50,
    number: 0,
  };

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj<PaymentService>('PaymentService', [
      'getMyPayments',
      'downloadInvoice',
    ]);
    paymentServiceSpy.getMyPayments.and.returnValue(of(mockPage));
    paymentServiceSpy.downloadInvoice.and.returnValue(
      of(new Blob(['%PDF-1.4'], { type: 'application/pdf' })),
    );

    await TestBed.configureTestingModule({
      imports: [
        MyPaymentsComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        { provide: PaymentService, useValue: paymentServiceSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(MyPaymentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('Test : tableau affiche les paiements et le total en haut', () => {
    expect(component).toBeTruthy();
    expect(paymentServiceSpy.getMyPayments).toHaveBeenCalled();

    const el: HTMLElement = fixture.nativeElement;
    const rows = el.querySelectorAll('[data-testid="payment-row"]');
    expect(rows.length).toBe(3);

    const totalEl = el.querySelector('[data-cy="total-payments-amount"]');
    expect(totalEl?.textContent).toContain('4,400.00 MAD');
  });

  it('Test : filtre statut filtre correctement le tableau et recalcule le total', () => {
    component.selectedStatus = 'SUCCESS';
    component.applyFilters();
    fixture.detectChanges();

    expect(component.filteredPayments.length).toBe(1);
    expect(component.filteredPayments[0].code).toBe('PAY-CMI101');
    expect(component.totalAmount).toBe(1400);

    const el: HTMLElement = fixture.nativeElement;
    const rows = el.querySelectorAll('[data-testid="payment-row"]');
    expect(rows.length).toBe(1);
  });

  it('Test : téléchargement facture appelle PaymentService.downloadInvoice()', () => {
    component.onDownloadInvoice(mockPayments[0]);
    expect(paymentServiceSpy.downloadInvoice).toHaveBeenCalledWith(101, 'FR');
  });

  it('devrait naviguer vers la réservation associée au clic sur voir la réservation', () => {
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    component.onViewReservation(mockPayments[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/reservations', 88]);
  });
});
