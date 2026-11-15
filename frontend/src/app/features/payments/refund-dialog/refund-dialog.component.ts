import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { TranslateModule } from '@ngx-translate/core';

import { PaymentResponse } from '../models/payment.model';

export interface RefundDialogData {
  payment: PaymentResponse;
}

export interface RefundDialogResult {
  montant?: number;
  raison: string;
}

@Component({
  selector: 'app-refund-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    TranslateModule,
  ],
  template: `
    <div class="refund-dialog-container" data-testid="refund-dialog">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon color="warn">replay</mat-icon>
        <span>Confirmer le Remboursement</span>
      </h2>

      <mat-dialog-content class="dialog-content">
        <div class="payment-summary">
          <p><strong>Code Paiement :</strong> {{ data.payment.code }}</p>
          <p><strong>Montant payé :</strong> {{ data.payment.montant | number: '1.2-2' }} {{ data.payment.devise || 'MAD' }}</p>
          <p><strong>Méthode :</strong> {{ data.payment.methode }}</p>
        </div>

        <form [formGroup]="form" class="refund-form">
          <div class="type-selection">
            <span class="section-label">Type de remboursement :</span>
            <mat-radio-group formControlName="refundType" class="radio-group" (change)="onTypeChange()">
              <mat-radio-button value="FULL">Remboursement Total ({{ data.payment.montant }} MAD)</mat-radio-button>
              <mat-radio-button value="PARTIAL">Remboursement Partiel</mat-radio-button>
            </mat-radio-group>
          </div>

          @if (form.get('refundType')?.value === 'PARTIAL') {
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Montant à rembourser (MAD)</mat-label>
              <input
                matInput
                type="number"
                formControlName="montant"
                min="1"
                [max]="data.payment.montant"
                placeholder="Ex: 500"
                data-testid="refund-amount-input"
              />
              @if (form.get('montant')?.hasError('required')) {
                <mat-error>Le montant est obligatoire.</mat-error>
              }
              @if (form.get('montant')?.hasError('max')) {
                <mat-error>Le montant ne peut pas dépasser {{ data.payment.montant }} MAD.</mat-error>
              }
              @if (form.get('montant')?.hasError('min')) {
                <mat-error>Le montant doit être supérieur à 0.</mat-error>
              }
            </mat-form-field>
          }

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Motif / Raison du remboursement</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="raison"
              placeholder="Ex: Annulation client sous 48h, véhicule indisponible..."
              data-testid="refund-reason-input"
            ></textarea>
            @if (form.get('raison')?.hasError('required')) {
              <mat-error>Le motif est obligatoire pour la traçabilité comptable.</mat-error>
            }
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button mat-dialog-close data-testid="btn-cancel-refund">
          Annuler
        </button>
        <button
          mat-flat-button
          color="warn"
          [disabled]="form.invalid"
          (click)="onConfirm()"
          data-testid="btn-confirm-refund"
        >
          Confirmer le Remboursement
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .refund-dialog-container {
        padding: 0.5rem;
        min-width: 320px;
        max-width: 500px;
      }
      .dialog-title {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 1rem;
      }
      .payment-summary {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 0.75rem 1rem;
        margin-bottom: 1rem;
        font-size: 0.9rem;
      }
      .payment-summary p {
        margin: 0.25rem 0;
      }
      .type-selection {
        margin-bottom: 1rem;
      }
      .section-label {
        display: block;
        font-weight: 600;
        font-size: 0.85rem;
        color: #334155;
        margin-bottom: 0.5rem;
      }
      .radio-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }
      .full-width {
        width: 100%;
        margin-bottom: 0.5rem;
      }
      .dialog-actions {
        padding-top: 1rem;
      }
    `,
  ],
})
export class RefundDialogComponent {
  readonly form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    public readonly dialogRef: MatDialogRef<RefundDialogComponent, RefundDialogResult>,
    @Inject(MAT_DIALOG_DATA) public readonly data: RefundDialogData,
  ) {
    this.form = this.fb.group({
      refundType: ['FULL', Validators.required],
      montant: [data.payment.montant, [Validators.min(1), Validators.max(data.payment.montant)]],
      raison: ['', [Validators.required, Validators.minLength(3)]],
    });
  }

  onTypeChange(): void {
    const isPartial = this.form.get('refundType')?.value === 'PARTIAL';
    const montantCtrl = this.form.get('montant');
    if (isPartial) {
      montantCtrl?.setValidators([
        Validators.required,
        Validators.min(1),
        Validators.max(this.data.payment.montant),
      ]);
    } else {
      montantCtrl?.clearValidators();
      montantCtrl?.setValue(this.data.payment.montant);
    }
    montantCtrl?.updateValueAndValidity();
  }

  onConfirm(): void {
    if (this.form.invalid) return;

    const isPartial = this.form.get('refundType')?.value === 'PARTIAL';
    const result: RefundDialogResult = {
      montant: isPartial ? Number(this.form.get('montant')?.value) : undefined,
      raison: this.form.get('raison')?.value?.trim(),
    };

    this.dialogRef.close(result);
  }
}
