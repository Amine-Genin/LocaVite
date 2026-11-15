import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';

import {
  PaymentFilterParams,
  PaymentMethodType,
  PaymentPageResponse,
  PaymentResponse,
  PaymentStatusType,
} from '../models/payment.model';
import { PaymentService } from '../services/payment.service';
import { PaymentStatsComponent, PaymentDashboardStats } from '../payment-stats/payment-stats.component';
import { RefundDialogComponent, RefundDialogResult } from '../refund-dialog/refund-dialog.component';
import { PaymentDetailsDialogComponent } from '../payment-details-dialog/payment-details-dialog.component';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    TranslateModule,
    PaymentStatsComponent,
  ],
  templateUrl: './admin-payments.component.html',
  styleUrl: './admin-payments.component.scss',
})
export class AdminPaymentsComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  readonly displayedColumns: string[] = [
    'id',
    'code',
    'client',
    'reservation',
    'montant',
    'methode',
    'statut',
    'date',
    'actions',
  ];

  readonly filterForm: FormGroup = this.fb.group({
    search: [''],
    statut: [''],
    methode: [''],
    dateFrom: [''],
    dateTo: [''],
  });

  readonly rawPayments = signal<PaymentResponse[]>([]);
  readonly filteredPayments = signal<PaymentResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Pagination
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);

  // Statistiques calculées
  readonly stats = signal<PaymentDashboardStats>({
    monthlyRevenue: 0,
    transactionCount: 0,
    successRate: 0,
    totalRefunded: 0,
    methodCounts: {},
    dailyRevenue: [],
  });

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const formValues = this.filterForm.value;
    const filterParams: PaymentFilterParams = {
      page: this.pageIndex(),
      size: this.pageSize(),
      statut: (formValues.statut as PaymentStatusType) || undefined,
      methode: (formValues.methode as PaymentMethodType) || undefined,
      dateFrom: formValues.dateFrom || undefined,
      dateTo: formValues.dateTo || undefined,
      sort: 'createdAt,desc',
    };

    this.paymentService.getAll(filterParams, this.pageIndex(), this.pageSize()).subscribe({
      next: (response: PaymentPageResponse) => {
        const items = response?.content ?? [];
        this.totalElements.set(response?.totalElements ?? items.length);
        this.rawPayments.set(items);
        this.applySearchFilter();
        this.calculateStats(items);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Erreur lors du chargement des paiements.');
      },
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.loadPayments();
  }

  resetFilters(): void {
    this.filterForm.reset({
      search: '',
      statut: '',
      methode: '',
      dateFrom: '',
      dateTo: '',
    });
    this.pageIndex.set(0);
    this.loadPayments();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadPayments();
  }

  private applySearchFilter(): void {
    const term = (this.filterForm.get('search')?.value || '').trim().toLowerCase();
    if (!term) {
      this.filteredPayments.set(this.rawPayments());
      return;
    }

    const filtered = this.rawPayments().filter((p) => {
      const codeMatch = (p.code || '').toLowerCase().includes(term);
      const clientMatch = (p.clientNom || '').toLowerCase().includes(term);
      const resMatch = String(p.reservationId || '').includes(term);
      return codeMatch || clientMatch || resMatch;
    });

    this.filteredPayments.set(filtered);
  }

  private calculateStats(items: PaymentResponse[]): void {
    const transactionCount = this.totalElements();
    const successful = items.filter((p) => p.statut === 'SUCCESS');
    const refunded = items.filter((p) => p.statut === 'REFUNDED' || p.statut === 'PARTIALLY_REFUNDED');

    const monthlyRevenue = successful.reduce((acc, curr) => acc + (Number(curr.montant) || 0), 0);
    const totalRefunded = refunded.reduce((acc, curr) => acc + (Number(curr.montant) || 0), 0);

    const successRate = items.length
      ? Math.round((successful.length / items.length) * 100)
      : 0;

    const methodCounts: Record<string, number> = {};
    items.forEach((p) => {
      const m = p.methode || 'AUTRE';
      methodCounts[m] = (methodCounts[m] || 0) + 1;
    });

    // Agrégation par date (JJ/MM) pour le graphique linéaire
    const revMap = new Map<string, number>();
    successful.forEach((p) => {
      const rawDate = p.paidAt || p.createdAt;
      const d = rawDate ? rawDate.slice(5, 10) : 'N/A';
      revMap.set(d, (revMap.get(d) || 0) + Number(p.montant));
    });

    const dailyRevenue: { date: string; amount: number }[] = [];
    revMap.forEach((amount, date) => {
      dailyRevenue.push({ date, amount });
    });

    this.stats.set({
      monthlyRevenue,
      transactionCount,
      successRate,
      totalRefunded,
      methodCounts,
      dailyRevenue: dailyRevenue.length ? dailyRevenue : [{ date: 'Aujourd\'hui', amount: monthlyRevenue }],
    });
  }

  // Actions
  onViewDetails(payment: PaymentResponse): void {
    this.dialog.open(PaymentDetailsDialogComponent, {
      width: '560px',
      data: { payment },
    });
  }

  onDownloadInvoice(payment: PaymentResponse): void {
    this.paymentService.downloadInvoice(payment.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facture-${payment.code}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      },
      error: () => {
        this.snackBar.open('Erreur lors du téléchargement de la facture.', 'Fermer', { duration: 3000 });
      },
    });
  }

  onConfirmCash(payment: PaymentResponse): void {
    if (!confirm(`Confirmer la réception de ${payment.montant} MAD en espèces pour le paiement ${payment.code} ?`)) {
      return;
    }

    this.paymentService.confirmCash(payment.id).subscribe({
      next: () => {
        this.snackBar.open(`Paiement espèces ${payment.code} confirmé avec succès !`, 'OK', { duration: 3000 });
        this.loadPayments();
      },
      error: () => {
        this.snackBar.open('Erreur lors de la confirmation du paiement en espèces.', 'Fermer', { duration: 4000 });
      },
    });
  }

  onOpenRefund(payment: PaymentResponse): void {
    const dialogRef = this.dialog.open(RefundDialogComponent, {
      width: '500px',
      data: { payment },
    });

    dialogRef.afterClosed().subscribe((result?: RefundDialogResult) => {
      if (!result) return;

      this.paymentService.refund(payment.id, result).subscribe({
        next: () => {
          this.snackBar.open(`Paiement ${payment.code} remboursé avec succès.`, 'OK', { duration: 3500 });
          this.loadPayments();
        },
        error: () => {
          this.snackBar.open('Échec du remboursement. Vérifiez le solde remboursable.', 'Fermer', { duration: 4000 });
        },
      });
    });
  }

  // Export CSV / Excel compatible
  exportCsv(): void {
    const list = this.filteredPayments();
    if (!list.length) {
      this.snackBar.open('Aucune transaction à exporter.', 'OK', { duration: 2500 });
      return;
    }

    const headers = ['ID', 'Code', 'Client', 'Réservation', 'Montant', 'Devise', 'Méthode', 'Statut', 'Date'];
    const rows = list.map((p) => [
      p.id,
      p.code,
      p.clientNom || `Client #${p.userId || '-'}`,
      `#${p.reservationId}`,
      p.montant,
      p.devise || 'MAD',
      p.methode,
      p.statut,
      p.paidAt || p.createdAt || '',
    ]);

    const csvContent =
      'data:text/csv;charset=utf-8,\uFEFF' +
      [headers.join(';'), ...rows.map((e) => e.join(';'))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `paiements-export-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

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
