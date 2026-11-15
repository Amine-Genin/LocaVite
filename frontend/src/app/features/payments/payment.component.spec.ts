import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { PaymentComponent } from './payment.component';
import { ReservationService } from '../reservations/services/reservation.service';
import { PaymentService } from './services/payment.service';
import { Reservation } from '../reservations/models/reservation.model';
import { PaymentResponse } from './models/payment.model';

describe('PaymentComponent', () => {
  let component: PaymentComponent;
  let fixture: ComponentFixture<PaymentComponent>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let router: Router;

  const mockReservation: Reservation = {
    id: 88,
    code: 'RES-8801',
    vehicleId: 1,
    vehicle: {
      id: 1,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      carburant: 'DIESEL',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      prixJour: 350,
      ville: 'Casablanca',
      statut: 'PUBLISHED',
    },
    dateDebut: '2026-12-01',
    dateFin: '2026-12-05',
    lieuPriseEnCharge: 'Aéroport Mohammed V',
    lieuRetour: 'Agence LocaVite Casablanca',
    prixTotal: 1400,
    statut: 'PENDING',
  };

  beforeEach(async () => {
    reservationServiceSpy = jasmine.createSpyObj<ReservationService>('ReservationService', [
      'getById',
    ]);
    paymentServiceSpy = jasmine.createSpyObj<PaymentService>(
      'PaymentService',
      ['initiate', 'redirectToCmi'],
      {
        companyRib: {
          banque: 'Attijariwafa Bank',
          titulaire: 'LOCAVITE MAROC SARL',
          rib: '007 780 0001234567890123 45',
          iban: 'MA64 0077 8000 0123 4567 8901 2345',
          swift: 'BCMAMAMC',
          agence: 'Casablanca Maarif',
        },
      },
    );

    reservationServiceSpy.getById.and.returnValue(of(mockReservation));

    await TestBed.configureTestingModule({
      imports: [
        PaymentComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: PaymentService, useValue: paymentServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ reservationId: '88' }),
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(PaymentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant et afficher le récapitulatif de la réservation', () => {
    expect(component).toBeTruthy();
    expect(reservationServiceSpy.getById).toHaveBeenCalledWith(88);

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-cy="summary-vehicle"]')?.textContent).toContain('Dacia Duster');
    expect(el.querySelector('[data-cy="summary-amount"]')?.textContent).toContain('1,400.00 MAD');
  });

  it('Test : affichage des modes disponibles (CMI, Espèces livraison, Espèces agence, Virement bancaire avec RIB)', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-cy="method-cmi"]')).toBeTruthy();
    expect(el.querySelector('[data-cy="method-cash-delivery"]')).toBeTruthy();
    expect(el.querySelector('[data-cy="method-cash-agency"]')).toBeTruthy();
    expect(el.querySelector('[data-cy="method-bank-transfer"]')).toBeTruthy();

    // Sélection du virement bancaire → affichage du RIB
    component.selectedMethod = 'BANK_TRANSFER';
    fixture.detectChanges();

    const ribCard = el.querySelector('[data-cy="bank-rib-card"]');
    expect(ribCard).toBeTruthy();
    expect(el.querySelector('[data-cy="rib-number"]')?.textContent).toContain(
      '007 780 0001234567890123 45',
    );
  });

  it('Test : sélection CMI → redirection vers l’URL CMI fournie par le backend', () => {
    const cmiUrl = 'https://testpayment.cmi.co.ma/fim/est3Dgate?oid=PAY-CMI88';
    const cmiResponse: PaymentResponse = {
      id: 42,
      code: 'PAY-CMI88',
      reservationId: 88,
      montant: 1400,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'PENDING',
      cmiRedirectUrl: cmiUrl,
    };

    paymentServiceSpy.initiate.and.returnValue(of(cmiResponse));
    component.selectedMethod = 'CMI';
    component.submitPayment();

    expect(paymentServiceSpy.initiate).toHaveBeenCalledWith(
      jasmine.objectContaining({
        reservationId: 88,
        montant: 1400,
        methode: 'CMI',
      }),
    );
    expect(paymentServiceSpy.redirectToCmi).toHaveBeenCalledWith(cmiUrl);
  });

  it('devrait rediriger vers /payment/success pour un paiement en espèces', () => {
    spyOn(router, 'navigate');
    const cashResponse: PaymentResponse = {
      id: 45,
      code: 'PAY-CSH45',
      reservationId: 88,
      montant: 1400,
      devise: 'MAD',
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
    };

    paymentServiceSpy.initiate.and.returnValue(of(cashResponse));
    component.selectedMethod = 'CASH_AGENCY';
    component.submitPayment();

    expect(router.navigate).toHaveBeenCalledWith(['/payment/success'], {
      queryParams: {
        paymentId: 45,
        reservationId: 88,
        code: 'PAY-CSH45',
        method: 'CASH_AGENCY',
        status: 'PENDING',
      },
    });
  });

  it('devrait afficher une bannière d’erreur si l’initialisation échoue', () => {
    paymentServiceSpy.initiate.and.returnValue(
      throwError(() => ({ error: { detail: 'Passerelle indisponible' } })),
    );

    component.selectedMethod = 'CMI';
    component.submitPayment();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Passerelle indisponible');
  });
});
