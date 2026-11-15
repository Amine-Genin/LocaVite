import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timer, of } from 'rxjs';
import { map, switchMap, takeWhile, timeout, catchError } from 'rxjs/operators';

import {
  BankRibInfo,
  CmiInitiateResponse,
  PaymentFilterParams,
  PaymentInitiateRequest,
  PaymentPageResponse,
  PaymentResponse,
  RefundResponse,
} from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/payments';

  readonly companyRib: BankRibInfo = {
    banque: 'Attijariwafa Bank',
    titulaire: 'LOCAVITE MAROC SARL',
    rib: '007 780 0001234567890123 45',
    iban: 'MA64 0077 8000 0123 4567 8901 2345',
    swift: 'BCMAMAMC',
    agence: 'Casablanca Maarif',
  };

  /**
   * Initialise un paiement selon le mode choisi :
   * - Si CMI : appelle POST /api/payments/cmi/initiate et retourne l'URL de redirection CMI
   * - Si CASH_DELIVERY ou CASH_AGENCY : appelle POST /api/payments/cash
   * - Si BANK_TRANSFER : appelle POST /api/payments
   */
  initiate(request: PaymentInitiateRequest): Observable<PaymentResponse> {
    if (request.methode === 'CMI') {
      const returnUrl =
        request.returnUrl || `${window.location.origin}/payment/success`;
      return this.http
        .post<CmiInitiateResponse>(`${this.apiUrl}/cmi/initiate`, {
          reservationId: request.reservationId,
          montant: request.montant,
          returnUrl,
        })
        .pipe(
          map((cmiRes) => ({
            id: cmiRes.paymentId,
            code: cmiRes.paymentCode,
            reservationId: request.reservationId,
            montant: cmiRes.montant,
            devise: cmiRes.devise || 'MAD',
            methode: 'CMI',
            statut: 'PENDING',
            cmiRedirectUrl: cmiRes.redirectUrl || cmiRes.gatewayUrl,
          })),
        );
    }

    if (
      request.methode === 'CASH_DELIVERY' ||
      request.methode === 'CASH_AGENCY'
    ) {
      return this.http.post<PaymentResponse>(`${this.apiUrl}/cash`, {
        reservationId: request.reservationId,
        montant: request.montant,
        methode: request.methode,
      });
    }

    return this.http.post<PaymentResponse>(this.apiUrl, {
      reservationId: request.reservationId,
      montant: request.montant,
      devise: request.devise || 'MAD',
      methode: request.methode,
      reference: request.reference,
    });
  }

  /**
   * Récupère l'historique paginé des paiements du client connecté (GET /api/payments/me).
   */
  getMyPayments(
    page = 0,
    size = 50,
    sort = 'createdAt,desc',
  ): Observable<PaymentPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http
      .get<PaymentPageResponse>(`${this.apiUrl}/me`, { params })
      .pipe(
        map((response) => ({
          ...response,
          content: (response?.content ?? []).map((payment) => ({
            ...payment,
            invoiceNumber:
              payment.invoiceNumber || this.extractInvoiceNumber(payment),
          })),
        })),
      );
  }

  /**
   * Récupère le statut d'un paiement par son identifiant.
   */
  getStatus(paymentId: number): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.apiUrl}/${paymentId}`).pipe(
      map((payment) => ({
        ...payment,
        invoiceNumber:
          payment.invoiceNumber || this.extractInvoiceNumber(payment),
      })),
    );
  }

  /**
   * Effectue un polling du statut du paiement toutes les 2 secondes pendant au maximum 30 secondes
   * jusqu'à ce que le statut devienne terminal (SUCCESS, FAILED, CANCELLED, REFUNDED).
   */
  pollStatus(
    paymentId: number,
    intervalMs = 2000,
    maxDurationMs = 30000,
  ): Observable<PaymentResponse> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.getStatus(paymentId)),
      takeWhile((payment) => !this.isTerminalStatus(payment.statut), true),
      timeout({
        each: maxDurationMs,
        with: () => this.getStatus(paymentId),
      }),
      catchError((err) => {
        return of({
          id: paymentId,
          code: `PAY-${paymentId}`,
          reservationId: 0,
          montant: 0,
          devise: 'MAD',
          methode: 'CMI',
          statut: 'FAILED',
          metadata: JSON.stringify({ error: err?.message || 'Timeout' }),
        } as PaymentResponse);
      }),
    );
  }

  /**
   * Récupère la liste paginée de tous les paiements (AGENCE / ADMIN).
   */
  getAll(
    filter?: PaymentFilterParams,
    page = 0,
    size = 20,
    sort = 'createdAt,desc',
  ): Observable<PaymentPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filter) {
      if (filter.statut) params = params.set('statut', filter.statut);
      if (filter.methode) params = params.set('methode', filter.methode);
      if (filter.reservationId) params = params.set('reservationId', filter.reservationId.toString());
      if (filter.userId) params = params.set('userId', filter.userId.toString());
      if (filter.code) params = params.set('code', filter.code);
      if (filter.dateFrom) params = params.set('dateDebut', filter.dateFrom);
      if (filter.dateTo) params = params.set('dateFin', filter.dateTo);
    }

    return this.http.get<PaymentPageResponse>(this.apiUrl, { params }).pipe(
      map((response) => ({
        ...response,
        content: (response?.content ?? []).map((payment) => ({
          ...payment,
          invoiceNumber: payment.invoiceNumber || this.extractInvoiceNumber(payment),
        })),
      })),
    );
  }

  /**
   * Confirme la réception d'un paiement en espèces (AGENCE / ADMIN).
   */
  confirmCash(paymentId: number): Observable<PaymentResponse> {
    return this.http.put<PaymentResponse>(`/api/admin/payments/${paymentId}/confirm-cash`, {});
  }

  /**
   * Rembourse totalement ou partiellement un client (AGENCE / ADMIN).
   */
  refund(paymentId: number, request: { montant?: number; raison: string }): Observable<RefundResponse> {
    return this.http.post<RefundResponse>(`/api/admin/payments/${paymentId}/refund`, request);
  }

  /**
   * Télécharge l'avoir PDF de remboursement (AGENCE / ADMIN).
   */
  downloadAvoir(paymentId: number): Observable<Blob> {
    return this.http.get(`/api/admin/payments/${paymentId}/avoir`, {
      responseType: 'blob',
    });
  }

  /**
   * Télécharge la facture PDF associée à un paiement confirmé.
   */
  downloadInvoice(paymentId: number, lang = 'FR'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${paymentId}/invoice`, {
      params: { lang: lang.toUpperCase() },
      responseType: 'blob',
    });
  }

  /**
   * Redirige le navigateur vers l'URL de la passerelle CMI.
   */
  redirectToCmi(cmiUrl: string): void {
    window.location.href = cmiUrl;
  }

  isTerminalStatus(status: string): boolean {
    return ['SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(
      status,
    );
  }

  extractInvoiceNumber(payment: PaymentResponse): string {
    if (payment.invoiceNumber) {
      return payment.invoiceNumber;
    }
    if (payment.metadata) {
      try {
        const meta = JSON.parse(payment.metadata);
        if (meta.invoiceNumber) {
          return String(meta.invoiceNumber);
        }
      } catch {
        // Ignorer si metadata n'est pas un JSON valide
      }
    }
    const year = payment.paidAt
      ? new Date(payment.paidAt).getFullYear()
      : new Date().getFullYear();
    const paddedId = String(payment.id || 1).padStart(5, '0');
    return `FAC-${year}-${paddedId}`;
  }
}
