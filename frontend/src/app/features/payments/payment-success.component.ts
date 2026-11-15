import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { PaymentService } from './services/payment.service';
import { PaymentResponse } from './models/payment.model';
import { LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  template: `
    <main
      class="payment-result-page"
      [attr.dir]="languageService.currentLang() === 'ar' ? 'rtl' : 'ltr'"
      data-cy="payment-success-page"
    >
      <mat-card class="result-card" data-cy="payment-success-card">
        @if (polling) {
          <div class="polling-state" data-cy="payment-polling-container">
            <mat-progress-spinner
              mode="indeterminate"
              diameter="52"
              data-cy="payment-status-spinner"
            ></mat-progress-spinner>
            <h2 data-cy="polling-title">{{ 'PAYMENT.SUCCESS.VERIFYING_TITLE' | translate }}</h2>
            <p>{{ 'PAYMENT.SUCCESS.VERIFYING_DESC' | translate }}</p>
          </div>
        } @else {
          <div class="result-icon success-icon" aria-hidden="true" data-cy="success-icon">
            <mat-icon>check_circle</mat-icon>
          </div>

          <h1 data-cy="payment-success-title">{{ 'PAYMENT.SUCCESS.TITLE' | translate }}</h1>
          <p class="result-message" data-cy="payment-success-message">
            {{ 'PAYMENT.SUCCESS.MESSAGE' | translate }}
          </p>

          <div class="details-box" data-cy="payment-details-box">
            @if (paymentCode) {
              <div class="detail-row">
                <span>{{ 'PAYMENT.SUCCESS.PAYMENT_CODE' | translate }}</span>
                <strong data-cy="payment-code">{{ paymentCode }}</strong>
              </div>
            }
            <div class="detail-row">
              <span>{{ 'PAYMENT.SUCCESS.INVOICE_NUMBER' | translate }}</span>
              <strong class="invoice-badge" data-cy="invoice-number">{{ invoiceNumber }}</strong>
            </div>
            @if (payment?.montant) {
              <div class="detail-row">
                <span>{{ 'PAYMENT.SUMMARY.TOTAL' | translate }}</span>
                <strong data-cy="payment-amount">
                  {{ payment?.montant | number: '1.2-2' }} {{ payment?.devise || 'MAD' }}
                </strong>
              </div>
            }
          </div>

          @if (downloadError) {
            <p class="download-error" data-cy="download-invoice-error">
              {{ downloadError | translate }}
            </p>
          }

          <div class="result-actions">
            <button
              mat-flat-button
              color="primary"
              type="button"
              [disabled]="downloadingInvoice || !paymentId"
              (click)="downloadInvoice()"
              data-cy="download-invoice-btn"
            >
              <mat-icon>download</mat-icon>
              <span>{{ 'PAYMENT.SUCCESS.DOWNLOAD_INVOICE' | translate }}</span>
            </button>

            <a
              mat-stroked-button
              routerLink="/my-reservations"
              data-cy="go-to-reservations-btn"
            >
              {{ 'PAYMENT.SUCCESS.MY_RESERVATIONS' | translate }}
            </a>
          </div>
        }
      </mat-card>
    </main>
  `,
  styles: [
    `
      .payment-result-page {
        min-height: 75vh;
        display: grid;
        place-items: center;
        padding: 2rem 1rem;
        background-color: #f8fafc;
      }
      .result-card {
        max-width: 560px;
        width: 100%;
        padding: 2.5rem 2rem;
        text-align: center;
        border-radius: 1.15rem;
        background: #ffffff;
        box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.12);
      }
      .polling-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        padding: 1.5rem 0;
      }
      .result-icon {
        width: 4.25rem;
        height: 4.25rem;
        margin: 0 auto 1.25rem;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: #dcfce7;
        color: #16a34a;
      }
      .result-icon mat-icon {
        font-size: 2.5rem;
        width: 2.5rem;
        height: 2.5rem;
      }
      h1 {
        font-size: 1.75rem;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 0.5rem;
      }
      .result-message {
        color: #475569;
        margin: 0 0 1.5rem;
        font-size: 1rem;
      }
      .details-box {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 0.85rem;
        padding: 1rem 1.25rem;
        margin-bottom: 1.5rem;
        display: grid;
        gap: 0.65rem;
      }
      .detail-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.95rem;
        color: #334155;
      }
      .invoice-badge {
        font-family: monospace;
        background: #dbeafe;
        color: #1e40af;
        padding: 0.2rem 0.6rem;
        border-radius: 0.4rem;
        font-size: 0.95rem;
      }
      .download-error {
        color: #dc2626;
        font-size: 0.9rem;
        margin-bottom: 1rem;
      }
      .result-actions {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 0.85rem;
      }
      @media (max-width: 480px) {
        .result-card {
          padding: 1.75rem 1.25rem;
        }
        .result-actions button,
        .result-actions a {
          width: 100%;
        }
      }
    `,
  ],
})
export class PaymentSuccessComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly paymentService = inject(PaymentService);
  readonly languageService = inject(LanguageService);

  paymentId = 0;
  reservationId = 0;
  paymentCode = '';
  invoiceNumber = '';
  payment: PaymentResponse | null = null;
  polling = false;
  downloadingInvoice = false;
  downloadError: string | null = null;

  private pollSub?: Subscription;

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    this.paymentId = Number(query.get('paymentId') || query.get('id') || 0);
    this.reservationId = Number(query.get('reservationId') || 0);
    this.paymentCode = query.get('code') || '';
    const explicitInvoice = query.get('invoiceNumber') || '';
    const method = query.get('method') || 'CMI';

    if (explicitInvoice) {
      this.invoiceNumber = explicitInvoice;
    } else if (this.paymentId) {
      const year = new Date().getFullYear();
      this.invoiceNumber = `FAC-${year}-${String(this.paymentId).padStart(5, '0')}`;
    } else {
      this.invoiceNumber = `FAC-${new Date().getFullYear()}-00001`;
    }

    if (this.paymentId) {
      if (method === 'CMI') {
        this.startPolling();
      } else {
        this.fetchStatusOnce();
      }
    }
  }

  startPolling(): void {
    this.polling = true;
    this.pollSub = this.paymentService
      .pollStatus(this.paymentId, 2000, 30000)
      .subscribe({
        next: (res) => {
          this.payment = res;
          if (res.code) {
            this.paymentCode = res.code;
          }
          if (res.reservationId) {
            this.reservationId = res.reservationId;
          }
          this.invoiceNumber = this.paymentService.extractInvoiceNumber(res);

          if (res.statut === 'FAILED' || res.statut === 'CANCELLED') {
            this.polling = false;
            void this.router.navigate(['/payment/failure'], {
              queryParams: {
                paymentId: this.paymentId,
                reservationId: this.reservationId || res.reservationId,
                error: 'PAYMENT_FAILED',
              },
            });
            return;
          }

          if (this.paymentService.isTerminalStatus(res.statut)) {
            this.polling = false;
          }
        },
        error: () => {
          this.polling = false;
        },
        complete: () => {
          this.polling = false;
        },
      });
  }

  fetchStatusOnce(): void {
    this.polling = true;
    this.pollSub = this.paymentService.getStatus(this.paymentId).subscribe({
      next: (res) => {
        this.payment = res;
        if (res.code) {
          this.paymentCode = res.code;
        }
        this.invoiceNumber = this.paymentService.extractInvoiceNumber(res);
        this.polling = false;
      },
      error: () => {
        this.polling = false;
      },
    });
  }

  downloadInvoice(): void {
    if (!this.paymentId || this.downloadingInvoice) {
      return;
    }
    this.downloadingInvoice = true;
    this.downloadError = null;
    const lang = this.languageService.currentLang().toUpperCase();

    this.paymentService.downloadInvoice(this.paymentId, lang).subscribe({
      next: (blob) => {
        this.downloadingInvoice = false;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${this.invoiceNumber || 'facture-' + this.paymentId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingInvoice = false;
        this.downloadError = 'PAYMENT.ERRORS.INVOICE_DOWNLOAD_FAILED';
      },
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }
}
