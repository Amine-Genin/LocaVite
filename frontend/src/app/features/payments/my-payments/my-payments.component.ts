import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

import { PaymentService } from '../services/payment.service';
import {
  PaymentMethodType,
  PaymentResponse,
  PaymentStatusType,
} from '../models/payment.model';
import { PaymentTableComponent } from '../payment-table/payment-table.component';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-my-payments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
    PaymentTableComponent,
  ],
  templateUrl: './my-payments.component.html',
  styleUrl: './my-payments.component.scss',
})
export class MyPaymentsComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly router = inject(Router);
  readonly languageService = inject(LanguageService);

  allPayments: PaymentResponse[] = [];
  filteredPayments: PaymentResponse[] = [];
  loading = true;
  errorMessage: string | null = null;
  downloadError: string | null = null;

  // Filtres : statut, méthode, date
  selectedStatus: PaymentStatusType | '' = '';
  selectedMethod: PaymentMethodType | '' = '';
  selectedDate = '';

  readonly statusOptions: PaymentStatusType[] = [
    'SUCCESS',
    'PENDING',
    'PROCESSING',
    'FAILED',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
    'CANCELLED',
  ];

  readonly methodOptions: PaymentMethodType[] = [
    'CMI',
    'CASH_DELIVERY',
    'CASH_AGENCY',
    'BANK_TRANSFER',
  ];

  ngOnInit(): void {
    this.loadMyPayments();
  }

  loadMyPayments(): void {
    this.loading = true;
    this.errorMessage = null;

    this.paymentService.getMyPayments(0, 100).subscribe({
      next: (page) => {
        this.allPayments = page?.content ?? [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'PAYMENT.HISTORY.LOAD_ERROR';
      },
    });
  }

  applyFilters(): void {
    this.filteredPayments = this.allPayments.filter((item) => {
      if (this.selectedStatus && item.statut !== this.selectedStatus) {
        return false;
      }
      if (this.selectedMethod && item.methode !== this.selectedMethod) {
        return false;
      }
      if (this.selectedDate) {
        const rawDate = item.paidAt || item.createdAt || '';
        if (!rawDate.startsWith(this.selectedDate)) {
          return false;
        }
      }
      return true;
    });
  }

  resetFilters(): void {
    this.selectedStatus = '';
    this.selectedMethod = '';
    this.selectedDate = '';
    this.applyFilters();
  }

  get totalAmount(): number {
    return this.filteredPayments.reduce(
      (sum, item) => sum + Number(item.montant || 0),
      0,
    );
  }

  get confirmedAmount(): number {
    return this.filteredPayments
      .filter(
        (item) =>
          item.statut === 'SUCCESS' || item.statut === 'PARTIALLY_REFUNDED',
      )
      .reduce((sum, item) => sum + Number(item.montant || 0), 0);
  }

  onDownloadInvoice(payment: PaymentResponse): void {
    this.downloadError = null;
    const lang = this.languageService.currentLang().toUpperCase();

    this.paymentService.downloadInvoice(payment.id, lang).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${payment.invoiceNumber || 'facture-' + payment.id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadError = 'PAYMENT.ERRORS.INVOICE_DOWNLOAD_FAILED';
      },
    });
  }

  onViewReservation(payment: PaymentResponse): void {
    void this.router.navigate(['/reservations', payment.reservationId]);
  }
}
