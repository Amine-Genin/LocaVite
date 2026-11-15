import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { PaymentTableComponent } from './payment-table.component';
import { PaymentResponse } from '../models/payment.model';

describe('PaymentTableComponent', () => {
  let component: PaymentTableComponent;
  let fixture: ComponentFixture<PaymentTableComponent>;

  const samplePayments: PaymentResponse[] = [
    {
      id: 1,
      code: 'PAY-AAA111',
      reservationId: 10,
      montant: 800,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      invoiceNumber: 'FAC-2026-00001',
      createdAt: '2026-11-10T12:00:00Z',
    },
    {
      id: 2,
      code: 'PAY-BBB222',
      reservationId: 11,
      montant: 500,
      devise: 'MAD',
      methode: 'CASH_DELIVERY',
      statut: 'PENDING',
      invoiceNumber: 'FAC-2026-00002',
      createdAt: '2026-11-11T12:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PaymentTableComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentTableComponent);
    component = fixture.componentInstance;
    component.payments = samplePayments;
    component.ngOnChanges({
      payments: new SimpleChange(null, samplePayments, true),
    });
    fixture.detectChanges();
  });

  it('affiche les colonnes date, reservation, montant, methode, statut, facture et les badges colorés', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    const rows = el.querySelectorAll('[data-testid="payment-row"]');
    expect(rows.length).toBe(2);

    const successBadge = el.querySelector('[data-cy="status-badge-SUCCESS"]');
    const pendingBadge = el.querySelector('[data-cy="status-badge-PENDING"]');
    expect(successBadge?.classList).toContain('status-success');
    expect(pendingBadge?.classList).toContain('status-pending');
  });

  it('émet downloadInvoice et viewReservation lors des clics d’action', () => {
    spyOn(component.downloadInvoice, 'emit');
    spyOn(component.viewReservation, 'emit');

    const el: HTMLElement = fixture.nativeElement;
    const downloadBtn = el.querySelector('[data-cy="download-invoice-1"]') as HTMLButtonElement;
    const viewResBtn = el.querySelector('[data-cy="view-reservation-1"]') as HTMLButtonElement;

    downloadBtn.click();
    viewResBtn.click();

    expect(component.downloadInvoice.emit).toHaveBeenCalledWith(samplePayments[0]);
    expect(component.viewReservation.emit).toHaveBeenCalledWith(samplePayments[0]);
  });
});
