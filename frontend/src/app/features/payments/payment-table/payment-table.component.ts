import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

import {
  PaymentMethodType,
  PaymentResponse,
  PaymentStatusType,
} from '../models/payment.model';

@Component({
  selector: 'app-payment-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
  ],
  template: `
    <div class="table-card" data-cy="payment-table-container">
      @if (dataSource.data.length > 0) {
        <div class="table-responsive">
          <table
            mat-table
            [dataSource]="dataSource"
            matSort
            class="payments-mat-table"
            data-cy="payments-table"
          >
            <!-- Colonne Date -->
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="createdAt">
                {{ 'PAYMENT.HISTORY.COLUMNS.DATE' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-date">
                <div class="date-cell">
                  <span class="primary-date">
                    {{ (item.paidAt || item.createdAt) | date: 'dd/MM/yyyy HH:mm' }}
                  </span>
                  <span class="payment-code-sub" data-cy="cell-code">{{ item.code }}</span>
                </div>
              </td>
            </ng-container>

            <!-- Colonne Réservation -->
            <ng-container matColumnDef="reservation">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="reservationId">
                {{ 'PAYMENT.HISTORY.COLUMNS.RESERVATION' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-reservation">
                <button
                  mat-button
                  color="primary"
                  type="button"
                  class="reservation-link-btn"
                  (click)="onViewReservation(item)"
                  [attr.data-cy]="'view-reservation-' + item.id"
                  data-testid="view-reservation-btn"
                >
                  <mat-icon>open_in_new</mat-icon>
                  <span>#{{ item.reservationId }}</span>
                </button>
              </td>
            </ng-container>

            <!-- Colonne Montant -->
            <ng-container matColumnDef="montant">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="montant">
                {{ 'PAYMENT.HISTORY.COLUMNS.AMOUNT' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-montant">
                <strong class="amount-value">
                  {{ item.montant | number: '1.2-2' }} {{ item.devise || 'MAD' }}
                </strong>
              </td>
            </ng-container>

            <!-- Colonne Méthode -->
            <ng-container matColumnDef="methode">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="methode">
                {{ 'PAYMENT.HISTORY.COLUMNS.METHOD' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-methode">
                <span class="method-pill">
                  <mat-icon class="method-pill-icon">{{ getMethodIcon(item.methode) }}</mat-icon>
                  <span>{{ getMethodLabelKey(item.methode) | translate }}</span>
                </span>
              </td>
            </ng-container>

            <!-- Colonne Statut (Badge coloré MatChip) -->
            <ng-container matColumnDef="statut">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="statut">
                {{ 'PAYMENT.HISTORY.COLUMNS.STATUS' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-statut">
                <mat-chip
                  class="status-chip"
                  [ngClass]="getStatusBadgeClass(item.statut)"
                  [attr.data-cy]="'status-badge-' + item.statut"
                >
                  {{ getStatusLabelKey(item.statut) | translate }}
                </mat-chip>
              </td>
            </ng-container>

            <!-- Colonne Facture & Actions -->
            <ng-container matColumnDef="facture">
              <th mat-header-cell *matHeaderCellDef>
                {{ 'PAYMENT.HISTORY.COLUMNS.INVOICE' | translate }}
              </th>
              <td mat-cell *matCellDef="let item" data-cy="cell-facture">
                <div class="invoice-actions">
                  @if (canDownloadInvoice(item.statut)) {
                    <span class="invoice-num" data-cy="cell-invoice-number">
                      {{ item.invoiceNumber }}
                    </span>
                    <button
                      mat-stroked-button
                      color="primary"
                      type="button"
                      class="download-btn"
                      (click)="onDownloadInvoice(item)"
                      [attr.data-cy]="'download-invoice-' + item.id"
                      data-testid="download-invoice-btn"
                    >
                      <mat-icon>download</mat-icon>
                      <span>{{ 'PAYMENT.HISTORY.ACTIONS.DOWNLOAD_PDF' | translate }}</span>
                    </button>
                  } @else {
                    <span class="no-invoice" data-cy="no-invoice-label">
                      {{ 'PAYMENT.HISTORY.NO_INVOICE' | translate }}
                    </span>
                  }

                  <button
                    mat-icon-button
                    color="primary"
                    type="button"
                    [matTooltip]="'PAYMENT.HISTORY.ACTIONS.VIEW_RESERVATION' | translate"
                    (click)="onViewReservation(item)"
                    [attr.data-cy]="'action-reservation-' + item.id"
                  >
                    <mat-icon>visibility</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr
              mat-row
              *matRowDef="let row; columns: displayedColumns"
              class="payment-row"
              [attr.data-cy]="'payment-row-' + row.id"
              data-testid="payment-row"
            ></tr>
          </table>
        </div>

        <mat-paginator
          [pageSizeOptions]="[5, 10, 20, 50]"
          [pageSize]="pageSize"
          showFirstLastButtons
          (page)="onPaginatorEvent($event)"
          aria-label="Pagination des paiements"
          data-cy="payments-paginator"
        ></mat-paginator>
      } @else {
        <div class="empty-table-state" data-cy="empty-payments-state">
          <mat-icon class="empty-icon">receipt_long</mat-icon>
          <h3>{{ 'PAYMENT.HISTORY.EMPTY_TITLE' | translate }}</h3>
          <p>{{ 'PAYMENT.HISTORY.EMPTY_DESC' | translate }}</p>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .table-card {
        background: #ffffff;
        border-radius: 1rem;
        box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08);
        overflow: hidden;
      }
      .table-responsive {
        overflow-x: auto;
      }
      .payments-mat-table {
        width: 100%;
      }
      .date-cell {
        display: flex;
        flex-direction: column;
        gap: 0.15rem;
        padding: 0.35rem 0;
      }
      .primary-date {
        font-weight: 500;
        color: #0f172a;
      }
      .payment-code-sub {
        font-size: 0.78rem;
        font-family: monospace;
        color: #64748b;
      }
      .reservation-link-btn {
        font-weight: 600;
      }
      .amount-value {
        color: #0f172a;
        font-size: 0.96rem;
      }
      .method-pill {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.9rem;
        color: #334155;
      }
      .method-pill-icon {
        font-size: 1.15rem;
        width: 1.15rem;
        height: 1.15rem;
        color: #2563eb;
      }
      .status-chip {
        font-weight: 600;
        font-size: 0.78rem;
      }
      .status-success {
        background-color: #dcfce7 !important;
        color: #166534 !important;
      }
      .status-pending,
      .status-processing {
        background-color: #fef9c3 !important;
        color: #854d0e !important;
      }
      .status-failed,
      .status-cancelled {
        background-color: #fee2e2 !important;
        color: #991b1b !important;
      }
      .status-refunded,
      .status-partially-refunded {
        background-color: #e0e7ff !important;
        color: #3730a3 !important;
      }
      .invoice-actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      .invoice-num {
        font-family: monospace;
        font-size: 0.82rem;
        color: #475569;
        background: #f1f5f9;
        padding: 0.15rem 0.45rem;
        border-radius: 0.35rem;
      }
      .no-invoice {
        font-size: 0.85rem;
        color: #94a3b8;
        font-style: italic;
      }
      .empty-table-state {
        text-align: center;
        padding: 3.5rem 1.5rem;
        color: #64748b;
      }
      .empty-icon {
        font-size: 3rem;
        width: 3rem;
        height: 3rem;
        color: #94a3b8;
        margin-bottom: 0.75rem;
      }
      .empty-table-state h3 {
        color: #0f172a;
        margin: 0 0 0.35rem;
        font-weight: 600;
      }
      .empty-table-state p {
        margin: 0;
      }
    `,
  ],
})
export class PaymentTableComponent implements OnChanges, AfterViewInit {
  @Input() payments: PaymentResponse[] = [];
  @Input() pageSize = 10;

  @Output() readonly downloadInvoice = new EventEmitter<PaymentResponse>();
  @Output() readonly viewReservation = new EventEmitter<PaymentResponse>();
  @Output() readonly pageChange = new EventEmitter<PageEvent>();

  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild(MatSort) sort?: MatSort;

  readonly displayedColumns: string[] = [
    'date',
    'reservation',
    'montant',
    'methode',
    'statut',
    'facture',
  ];

  readonly dataSource = new MatTableDataSource<PaymentResponse>([]);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['payments']) {
      this.dataSource.data = this.payments ?? [];
      if (this.paginator) {
        this.dataSource.paginator = this.paginator;
      }
      if (this.sort) {
        this.dataSource.sort = this.sort;
      }
    }
  }

  ngAfterViewInit(): void {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  onDownloadInvoice(payment: PaymentResponse): void {
    this.downloadInvoice.emit(payment);
  }

  onViewReservation(payment: PaymentResponse): void {
    this.viewReservation.emit(payment);
  }

  onPaginatorEvent(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  canDownloadInvoice(statut: PaymentStatusType | string): boolean {
    return (
      statut === 'SUCCESS' ||
      statut === 'REFUNDED' ||
      statut === 'PARTIALLY_REFUNDED'
    );
  }

  getStatusBadgeClass(statut: PaymentStatusType | string): string {
    switch (statut) {
      case 'SUCCESS':
        return 'status-success';
      case 'PENDING':
        return 'status-pending';
      case 'PROCESSING':
        return 'status-processing';
      case 'FAILED':
        return 'status-failed';
      case 'CANCELLED':
        return 'status-cancelled';
      case 'REFUNDED':
        return 'status-refunded';
      case 'PARTIALLY_REFUNDED':
        return 'status-partially-refunded';
      default:
        return 'status-pending';
    }
  }

  getStatusLabelKey(statut: PaymentStatusType | string): string {
    return `PAYMENT.HISTORY.STATUS.${statut}`;
  }

  getMethodLabelKey(methode: PaymentMethodType | string): string {
    return `PAYMENT.HISTORY.METHODS.${methode}`;
  }

  getMethodIcon(methode: PaymentMethodType | string): string {
    switch (methode) {
      case 'CMI':
        return 'credit_card';
      case 'CASH_DELIVERY':
        return 'local_shipping';
      case 'CASH_AGENCY':
        return 'storefront';
      case 'BANK_TRANSFER':
        return 'account_balance';
      default:
        return 'payments';
    }
  }
}
