export type PaymentMethodType =
  | 'CMI'
  | 'CASH_DELIVERY'
  | 'CASH_AGENCY'
  | 'BANK_TRANSFER';

export type PaymentStatusType =
  | 'PENDING'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'REFUNDED'
  | 'PARTIALLY_REFUNDED'
  | 'CANCELLED';

export interface PaymentInitiateRequest {
  reservationId: number;
  montant: number;
  methode: PaymentMethodType;
  devise?: string;
  returnUrl?: string;
  reference?: string;
}

export interface CmiInitiateResponse {
  paymentId: number;
  paymentCode: string;
  montant: number;
  devise: string;
  gatewayUrl: string;
  redirectUrl: string;
  formData?: Record<string, string>;
  expiresAt?: string;
}

export interface PaymentResponse {
  id: number;
  code: string;
  reservationId: number;
  userId?: number;
  montant: number;
  devise: string;
  methode: PaymentMethodType;
  statut: PaymentStatusType;
  reference?: string;
  invoiceNumber?: string;
  transactionId?: string;
  metadata?: string;
  cmiRedirectUrl?: string;
  createdAt?: string;
  updatedAt?: string;
  paidAt?: string;
}

export interface BankRibInfo {
  banque: string;
  titulaire: string;
  rib: string;
  iban: string;
  swift: string;
  agence: string;
}

export interface PaymentFilterParams {
  statut?: PaymentStatusType | '';
  methode?: PaymentMethodType | '';
  date?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PaymentPageResponse {
  content: PaymentResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}
