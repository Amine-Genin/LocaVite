import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { AdminPaymentsComponent } from './admin-payments.component';
import { PaymentService } from '../services/payment.service';
import { PaymentPageResponse, PaymentResponse } from '../models/payment.model';

describe('AdminPaymentsComponent', () => {
  let component: AdminPaymentsComponent;
  let fixture: ComponentFixture<AdminPaymentsComponent>;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const mockPayments: PaymentResponse[] = [
    {
      id: 1,
      code: 'PAY-2026-00001',
      reservationId: 101,
      userId: 10,
      clientNom: 'Karim Alaoui',
      montant: 1500,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      paidAt: '2026-11-10T10:00:00Z',
    },
    {
      id: 2,
      code: 'PAY-2026-00002',
      reservationId: 102,
      userId: 11,
      clientNom: 'Fatima Zahra',
      montant: 800,
      devise: 'MAD',
      methode: 'CASH_DELIVERY',
      statut: 'PENDING',
      createdAt: '2026-11-12T14:30:00Z',
    },
    {
      id: 3,
      code: 'PAY-2026-00003',
      reservationId: 103,
      userId: 12,
      clientNom: 'Youssef Bennani',
      montant: 2000,
      devise: 'MAD',
      methode: 'BANK_TRANSFER',
      statut: 'REFUNDED',
      createdAt: '2026-11-05T09:00:00Z',
    },
  ];

  const mockPageResponse: PaymentPageResponse = {
    content: mockPayments,
    totalElements: 3,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', [
      'getAll',
      'confirmCash',
      'refund',
      'downloadInvoice',
    ]);
    const mockDialogRef = {
      afterClosed: () => of(null),
      close: jasmine.createSpy('close'),
    };
    dialogSpy = jasmine.createSpyObj('MatDialog', {
      open: mockDialogRef,
    });
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    paymentServiceSpy.getAll.and.returnValue(of(mockPageResponse));
    paymentServiceSpy.confirmCash.and.returnValue(of({ ...mockPayments[1], statut: 'SUCCESS' }));
    paymentServiceSpy.downloadInvoice.and.returnValue(of(new Blob(['pdf content'], { type: 'application/pdf' })));

    await TestBed.configureTestingModule({
      imports: [
        AdminPaymentsComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    })
      .overrideComponent(AdminPaymentsComponent, {
        set: {
          providers: [
            { provide: MatDialog, useValue: dialogSpy },
            { provide: MatSnackBar, useValue: snackBarSpy },
          ],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AdminPaymentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load all payments into table', () => {
    expect(component).toBeTruthy();
    expect(paymentServiceSpy.getAll).toHaveBeenCalled();
    expect(component.filteredPayments().length).toBe(3);
    expect(component.totalElements()).toBe(3);
  });

  it('should calculate dashboard statistics correctly', () => {
    const stats = component.stats();
    expect(stats.transactionCount).toBe(3);
    expect(stats.monthlyRevenue).toBe(1500); // Seul SUCCESS a 1500
    expect(stats.totalRefunded).toBe(2000); // 1 REFUNDED avec 2000
    expect(stats.successRate).toBe(33); // 1 sur 3
  });

  it('should filter payments when searching by client name or code', () => {
    component.filterForm.patchValue({ search: 'Karim' });
    (component as unknown as { applySearchFilter: () => void }).applySearchFilter();
    expect(component.filteredPayments().length).toBe(1);
    expect(component.filteredPayments()[0].code).toBe('PAY-2026-00001');

    component.filterForm.patchValue({ search: 'PAY-2026-00002' });
    (component as unknown as { applySearchFilter: () => void }).applySearchFilter();
    expect(component.filteredPayments().length).toBe(1);
    expect(component.filteredPayments()[0].clientNom).toBe('Fatima Zahra');
  });

  it('should open payment details dialog', () => {
    component.onViewDetails(mockPayments[0]);
    expect(dialogSpy.open).toHaveBeenCalled();
  });

  it('should open refund dialog and process refund on dialog close with result', () => {
    paymentServiceSpy.refund.and.returnValue(
      of({
        id: 1,
        paymentId: 1,
        reservationId: 101,
        montant: 1500,
        statut: 'REFUNDED',
        methode: 'CMI',
        raison: 'Client mécontent',
        dateOperation: new Date().toISOString(),
      }),
    );
    const dialogRefMock = {
      afterClosed: () => of({ montant: 1500, raison: 'Client mécontent' }),
    };
    dialogSpy.open.and.returnValue(dialogRefMock as unknown as ReturnType<typeof dialogSpy.open>);

    component.onOpenRefund(mockPayments[0]);

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(paymentServiceSpy.refund).toHaveBeenCalledWith(1, {
      montant: 1500,
      raison: 'Client mécontent',
    });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/remboursé avec succès/),
      'OK',
      jasmine.any(Object),
    );
  });

  it('should confirm cash payment when confirmed by agency', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.onConfirmCash(mockPayments[1]);

    expect(paymentServiceSpy.confirmCash).toHaveBeenCalledWith(2);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/confirmé avec succès/),
      'OK',
      jasmine.any(Object),
    );
  });

  it('should export payments to CSV', () => {
    const createElementSpy = spyOn(document, 'createElement').and.callThrough();
    component.exportCsv();
    expect(createElementSpy).toHaveBeenCalledWith('a');
  });

  it('should display error message when loading payments fails', () => {
    paymentServiceSpy.getAll.and.returnValue(throwError(() => new Error('Server error')));
    component.loadPayments();
    expect(component.errorMessage()).toBe('Erreur lors du chargement des paiements.');
    expect(component.isLoading()).toBeFalse();
  });
});
