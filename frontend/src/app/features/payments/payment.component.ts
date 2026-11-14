import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';

import { ReservationService } from '../reservations/services/reservation.service';
import { Reservation } from '../reservations/models/reservation.model';
import { PaymentService } from './services/payment.service';
import { BankRibInfo, PaymentMethodType } from './models/payment.model';
import { LanguageService } from '../../core/i18n/language.service';

export interface PaymentMethodOption {
  value: PaymentMethodType;
  labelKey: string;
  descKey: string;
  icon: string;
  badgeKey?: string;
  dataCy: string;
}

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatRadioModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.scss',
})
export class PaymentComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly reservationService = inject(ReservationService);
  private readonly paymentService = inject(PaymentService);
  readonly languageService = inject(LanguageService);

  reservationId = 0;
  reservation: Reservation | null = null;
  selectedMethod: PaymentMethodType = 'CMI';
  loadingReservation = true;
  processingPayment = false;
  errorMessage: string | null = null;
  ribCopied = false;

  readonly ribInfo: BankRibInfo = this.paymentService.companyRib;

  readonly paymentMethods: PaymentMethodOption[] = [
    {
      value: 'CMI',
      labelKey: 'PAYMENT.METHODS.CMI.TITLE',
      descKey: 'PAYMENT.METHODS.CMI.DESC',
      icon: 'credit_card',
      badgeKey: 'PAYMENT.METHODS.CMI.BADGE',
      dataCy: 'method-cmi',
    },
    {
      value: 'CASH_DELIVERY',
      labelKey: 'PAYMENT.METHODS.CASH_DELIVERY.TITLE',
      descKey: 'PAYMENT.METHODS.CASH_DELIVERY.DESC',
      icon: 'local_shipping',
      dataCy: 'method-cash-delivery',
    },
    {
      value: 'CASH_AGENCY',
      labelKey: 'PAYMENT.METHODS.CASH_AGENCY.TITLE',
      descKey: 'PAYMENT.METHODS.CASH_AGENCY.DESC',
      icon: 'storefront',
      dataCy: 'method-cash-agency',
    },
    {
      value: 'BANK_TRANSFER',
      labelKey: 'PAYMENT.METHODS.BANK_TRANSFER.TITLE',
      descKey: 'PAYMENT.METHODS.BANK_TRANSFER.DESC',
      icon: 'account_balance',
      dataCy: 'method-bank-transfer',
    },
  ];

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('reservationId');
    this.reservationId = Number(idParam || 0);
    if (!this.reservationId || Number.isNaN(this.reservationId)) {
      this.loadingReservation = false;
      this.errorMessage = 'PAYMENT.ERRORS.INVALID_RESERVATION';
      return;
    }
    this.loadReservation();
  }

  loadReservation(): void {
    this.loadingReservation = true;
    this.errorMessage = null;
    this.reservationService.getById(this.reservationId).subscribe({
      next: (res) => {
        this.reservation = res;
        this.loadingReservation = false;
      },
      error: () => {
        this.loadingReservation = false;
        this.errorMessage = 'PAYMENT.ERRORS.LOAD_FAILED';
      },
    });
  }

  submitPayment(): void {
    if (!this.reservation || this.processingPayment) {
      return;
    }

    this.processingPayment = true;
    this.errorMessage = null;

    this.paymentService
      .initiate({
        reservationId: this.reservation.id,
        montant: this.reservation.prixTotal,
        methode: this.selectedMethod,
        devise: 'MAD',
        returnUrl: `${window.location.origin}/payment/success?reservationId=${this.reservation.id}`,
      })
      .subscribe({
        next: (response) => {
          this.processingPayment = false;
          if (this.selectedMethod === 'CMI' && response.cmiRedirectUrl) {
            this.redirectToCmi(response.cmiRedirectUrl);
            return;
          }

          this.router.navigate(['/payment/success'], {
            queryParams: {
              paymentId: response.id,
              reservationId: this.reservation?.id,
              code: response.code,
              method: this.selectedMethod,
              status: response.statut,
            },
          });
        },
        error: (err) => {
          this.processingPayment = false;
          const detail =
            err?.error?.detail ||
            err?.error?.message ||
            'PAYMENT.ERRORS.INITIATE_FAILED';
          this.errorMessage = detail;
        },
      });
  }

  redirectToCmi(cmiUrl: string): void {
    this.paymentService.redirectToCmi(cmiUrl);
  }

  copyRib(): void {
    if (navigator?.clipboard) {
      navigator.clipboard.writeText(this.ribInfo.rib);
    }
    this.ribCopied = true;
    setTimeout(() => {
      this.ribCopied = false;
    }, 2500);
  }

  get vehicleDisplayName(): string {
    if (!this.reservation) {
      return '';
    }
    if (this.reservation.vehicle) {
      return `${this.reservation.vehicle.marque} ${this.reservation.vehicle.modele}`;
    }
    return `Véhicule #${this.reservation.vehicleId || this.reservation.id}`;
  }
}
