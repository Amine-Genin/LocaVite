import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

import { LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-payment-failure',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  template: `
    <main
      class="payment-result-page"
      [attr.dir]="languageService.currentLang() === 'ar' ? 'rtl' : 'ltr'"
      data-cy="payment-failure-page"
    >
      <mat-card class="result-card" data-cy="payment-failure-card">
        <div class="result-icon failure-icon" aria-hidden="true" data-cy="failure-icon">
          <mat-icon>error_outline</mat-icon>
        </div>

        <h1 data-cy="payment-failure-title">{{ 'PAYMENT.FAILURE.TITLE' | translate }}</h1>
        <p class="result-message" data-cy="payment-failure-message">
          {{ 'PAYMENT.FAILURE.MESSAGE' | translate }}
        </p>

        @if (errorReason) {
          <div class="error-reason-box" data-cy="payment-failure-reason">
            {{ errorReason }}
          </div>
        }

        <div class="result-actions">
          <button
            mat-flat-button
            color="primary"
            type="button"
            (click)="retryPayment()"
            data-cy="retry-payment-btn"
          >
            <mat-icon>refresh</mat-icon>
            <span>{{ 'PAYMENT.FAILURE.RETRY_BTN' | translate }}</span>
          </button>

          <a
            mat-stroked-button
            routerLink="/my-reservations"
            data-cy="back-to-reservations-btn"
          >
            {{ 'PAYMENT.SUCCESS.MY_RESERVATIONS' | translate }}
          </a>
        </div>
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
        max-width: 540px;
        width: 100%;
        padding: 2.5rem 2rem;
        text-align: center;
        border-radius: 1.15rem;
        background: #ffffff;
        box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.12);
      }
      .result-icon {
        width: 4.25rem;
        height: 4.25rem;
        margin: 0 auto 1.25rem;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: #fee2e2;
        color: #dc2626;
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
        margin: 0 0 1.25rem;
        font-size: 1rem;
      }
      .error-reason-box {
        background: #fef2f2;
        border: 1px solid #fecaca;
        color: #991b1b;
        border-radius: 0.75rem;
        padding: 0.75rem 1rem;
        margin-bottom: 1.5rem;
        font-size: 0.9rem;
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
export class PaymentFailureComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly languageService = inject(LanguageService);

  reservationId = 0;
  paymentId = 0;
  errorReason: string | null = null;

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    this.reservationId = Number(query.get('reservationId') || 0);
    this.paymentId = Number(query.get('paymentId') || 0);
    this.errorReason = query.get('error') || query.get('reason') || null;
  }

  retryPayment(): void {
    if (this.reservationId > 0) {
      void this.router.navigate(['/payment', this.reservationId]);
    } else {
      void this.router.navigate(['/my-reservations']);
    }
  }
}
