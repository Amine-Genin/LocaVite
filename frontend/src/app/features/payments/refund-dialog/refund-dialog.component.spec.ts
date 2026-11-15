import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { RefundDialogComponent, RefundDialogData } from './refund-dialog.component';
import { PaymentResponse } from '../models/payment.model';

describe('RefundDialogComponent', () => {
  let component: RefundDialogComponent;
  let fixture: ComponentFixture<RefundDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<RefundDialogComponent>>;

  const mockPayment: PaymentResponse = {
    id: 10,
    code: 'PAY-2026-00010',
    reservationId: 5,
    montant: 1200,
    devise: 'MAD',
    methode: 'CMI',
    statut: 'SUCCESS',
  };

  const dialogData: RefundDialogData = {
    payment: mockPayment,
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [RefundDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        FormBuilder,
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RefundDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize default form values for full refund', () => {
    expect(component).toBeTruthy();
    expect(component.form.get('refundType')?.value).toBe('FULL');
    expect(component.form.get('montant')?.value).toBe(1200);
    expect(component.form.valid).toBeFalse(); // Car la raison est vide
  });

  it('should validate form and close dialog with total refund when confirmed', () => {
    component.form.patchValue({
      raison: 'Annulation demandée par le client',
    });
    expect(component.form.valid).toBeTrue();

    component.onConfirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      montant: undefined,
      raison: 'Annulation demandée par le client',
    });
  });

  it('should validate partial refund amount within allowed range', () => {
    component.form.patchValue({
      refundType: 'PARTIAL',
      montant: 500,
      raison: 'Remboursement partiel caution',
    });
    component.onTypeChange();

    expect(component.form.valid).toBeTrue();
    component.onConfirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      montant: 500,
      raison: 'Remboursement partiel caution',
    });
  });

  it('should invalidate partial refund if amount exceeds initial payment', () => {
    component.form.patchValue({
      refundType: 'PARTIAL',
      montant: 2000,
      raison: 'Trop élevé',
    });
    component.onTypeChange();

    expect(component.form.invalid).toBeTrue();
    expect(component.form.get('montant')?.hasError('max')).toBeTrue();
  });
});
