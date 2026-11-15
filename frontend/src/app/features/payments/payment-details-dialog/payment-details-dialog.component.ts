import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';

import { PaymentResponse } from '../models/payment.model';

export interface PaymentDetailsDialogData {
  payment: PaymentResponse;
}

@Component({
  selector: 'app-payment-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    TranslateModule,
  ],
  template: `
    <div class="details-dialog-container" data-testid="payment-details-dialog">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon color="primary">receipt</mat-icon>
        <span>Détails du Paiement #{{ data.payment.id }}</span>
      </h2>

      <mat-dialog-content class="dialog-content">
        <div class="info-grid">
          <div class="info-item">
            <span class="label">Code Paiement</span>
            <span class="value code-val">{{ data.payment.code }}</span>
          </div>

          <div class="info-item">
            <span class="label">Réservation Associée</span>
            <span class="value">#{{ data.payment.reservationId }}</span>
          </div>

          <div class="info-item">
            <span class="label">Client</span>
            <span class="value">{{ data.payment.clientNom || ('ID ' + (data.payment.userId || '-')) }}</span>
          </div>

          <div class="info-item">
            <span class="label">Montant</span>
            <span class="value amount-val">
              {{ data.payment.montant | number: '1.2-2' }} {{ data.payment.devise || 'MAD' }}
            </span>
          </div>

          <div class="info-item">
            <span class="label">Méthode</span>
            <span class="value method-val">
              <mat-icon class="inline-icon">{{ getMethodIcon(data.payment.methode) }}</mat-icon>
              {{ data.payment.methode }}
            </span>
          </div>

          <div class="info-item">
            <span class="label">Statut</span>
            <span class="value">
              <mat-chip class="status-chip" [ngClass]="getStatusClass(data.payment.statut)">
                {{ data.payment.statut }}
              </mat-chip>
            </span>
          </div>

          <div class="info-item">
            <span class="label">Date de Création</span>
            <span class="value">{{ data.payment.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</span>
          </div>

          <div class="info-item">
            <span class="label">Date de Paiement</span>
            <span class="value">{{ (data.payment.paidAt | date: 'dd/MM/yyyy HH:mm:ss') || '-' }}</span>
          </div>

          <div class="info-item" *ngIf="data.payment.invoiceNumber">
            <span class="label">Numéro de Facture</span>
            <span class="value invoice-val">{{ data.payment.invoiceNumber }}</span>
          </div>

          <div class="info-item" *ngIf="data.payment.transactionId">
            <span class="label">Transaction ID (Passerelle)</span>
            <span class="value">{{ data.payment.transactionId }}</span>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close data-testid="btn-close-details">
          Fermer
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .details-dialog-container {
        padding: 0.5rem;
        min-width: 320px;
        max-width: 550px;
      }
      .dialog-title {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 1rem;
      }
      .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 1rem;
        padding: 0.5rem 0;
      }
      .info-item {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .label {
        font-size: 0.78rem;
        font-weight: 600;
        text-transform: uppercase;
        color: #64748b;
      }
      .value {
        font-size: 0.95rem;
        font-weight: 500;
        color: #0f172a;
      }
      .code-val,
      .invoice-val {
        font-family: monospace;
        font-weight: 600;
      }
      .amount-val {
        font-weight: 700;
        color: #047857;
      }
      .method-val {
        display: flex;
        align-items: center;
        gap: 0.35rem;
      }
      .inline-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
      .status-chip {
        font-size: 0.75rem;
        font-weight: 600;
      }
      .chip-success {
        background-color: #d1fae5 !important;
        color: #065f46 !important;
      }
      .chip-pending {
        background-color: #fef3c7 !important;
        color: #92400e !important;
      }
      .chip-failed {
        background-color: #fee2e2 !important;
        color: #991b1b !important;
      }
      .chip-refunded {
        background-color: #e0e7ff !important;
        color: #3730a3 !important;
      }
    `,
  ],
})
export class PaymentDetailsDialogComponent {
  constructor(
    public readonly dialogRef: MatDialogRef<PaymentDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PaymentDetailsDialogData,
  ) {}

  getMethodIcon(method: string): string {
    switch (method) {
      case 'CMI':
        return 'credit_card';
      case 'CASH_DELIVERY':
      case 'CASH_AGENCY':
        return 'payments';
      case 'BANK_TRANSFER':
        return 'account_balance';
      default:
        return 'payment';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'chip-success';
      case 'PENDING':
      case 'PROCESSING':
        return 'chip-pending';
      case 'FAILED':
      case 'CANCELLED':
        return 'chip-failed';
      case 'REFUNDED':
      case 'PARTIALLY_REFUNDED':
        return 'chip-refunded';
      default:
        return '';
    }
  }
}
